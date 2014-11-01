/*
 * Copyright 2010-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.lang.types.lang

import kotlin.Function0
import org.jetbrains.jet.descriptors.serialization.*
import org.jetbrains.jet.descriptors.serialization.context.DeserializationContext
import org.jetbrains.jet.descriptors.serialization.descriptors.AnnotationLoader
import org.jetbrains.jet.descriptors.serialization.descriptors.ConstantLoader
import org.jetbrains.jet.descriptors.serialization.descriptors.DeserializedPackageMemberScope
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor
import org.jetbrains.jet.lang.descriptors.PackageFragmentDescriptor
import org.jetbrains.jet.lang.descriptors.PackageFragmentProvider
import org.jetbrains.jet.lang.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.jet.lang.resolve.name.ClassId
import org.jetbrains.jet.lang.resolve.name.FqName
import org.jetbrains.jet.lang.resolve.name.Name
import org.jetbrains.jet.lang.resolve.scopes.JetScope
import org.jetbrains.jet.storage.StorageManager

import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.util.ArrayList
import java.util.Collections

class BuiltinsPackageFragment(storageManager: StorageManager, module: ModuleDescriptor)
  : PackageFragmentDescriptorImpl(module, KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME) {

    private val members: DeserializedPackageMemberScope
    private val nameResolver: NameResolver
    public val provider: PackageFragmentProvider

    {
        nameResolver = NameSerializationUtil.deserializeNameResolver(getStream(BuiltInsSerializationUtil.getNameTableFilePath(fqName)))

        provider = BuiltinsPackageFragmentProvider()

        val classNames = object : Function0<Collection<Name>> {
            override fun invoke(): Collection<Name> {
                val `in` = getStream(BuiltInsSerializationUtil.getClassNamesFilePath(fqName))

                try {
                    val data = DataInputStream(`in`)
                    try {
                        val size = data.readInt()
                        val result = ArrayList<Name>(size)
                        for (i in 0..size - 1) {
                            result.add(nameResolver.getName(data.readInt()))
                        }
                        return result
                    }
                    finally {
                        data.close()
                    }
                }
                catch (e: IOException) {
                    throw IllegalStateException(e)
                }

            }
        }

        val builtInsClassDataFinder = BuiltInsClassDataFinder()
        val deserializationContext = DeserializationContext(storageManager, module, builtInsClassDataFinder, // TODO: support annotations
                                                            AnnotationLoader.UNSUPPORTED, ConstantLoader.UNSUPPORTED, provider, FlexibleTypeCapabilitiesDeserializer.ThrowException, ClassDeserializer(storageManager, builtInsClassDataFinder), nameResolver)
        members = DeserializedPackageMemberScope(this, loadPackage(), deserializationContext, classNames)
    }

    private fun loadPackage(): ProtoBuf.Package {
        val packageFilePath = BuiltInsSerializationUtil.getPackageFilePath(fqName)
        val stream = getStream(packageFilePath)
        try {
            return ProtoBuf.Package.parseFrom(stream)
        }
        catch (e: IOException) {
            throw IllegalStateException(e)
        }

    }

    override fun getMemberScope(): JetScope {
        return members
    }

    private fun getStream(path: String): InputStream {
        val stream = getStreamNullable(path)
        if (stream == null) {
            throw IllegalStateException("Resource not found in classpath: " + path)
        }
        return stream
    }

    private fun getStreamNullable(path: String): InputStream? {
        //noinspection ConstantConditions
        return javaClass<KotlinBuiltIns>().getClassLoader().getResourceAsStream(path)
    }

    private inner class BuiltinsPackageFragmentProvider : PackageFragmentProvider {
        override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
            if (KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME == fqName) {
                return listOf<PackageFragmentDescriptor>(this@BuiltinsPackageFragment)
            }
            return listOf()
        }

        override fun getSubPackagesOf(fqName: FqName): Collection<FqName> {
            if (fqName.isRoot()) {
                return setOf(KotlinBuiltIns.BUILT_INS_PACKAGE_FQ_NAME)
            }
            return listOf()
        }
    }

    private inner class BuiltInsClassDataFinder : ClassDataFinder {
        override fun findClassData(classId: ClassId): ClassData? {
            val metadataPath = BuiltInsSerializationUtil.getClassMetadataPath(classId)
            if (metadataPath == null) return null
            val stream = getStreamNullable(metadataPath)
            if (stream == null) return null

            try {
                val classProto = ProtoBuf.Class.parseFrom(stream)

                val expectedShortName = classId.getRelativeClassName().shortName()
                val actualShortName = nameResolver.getClassId(classProto.getFqName()).getRelativeClassName().shortName()
                if (!actualShortName.isSpecial() && actualShortName != expectedShortName) {
                    // Workaround for case-insensitive file systems,
                    // otherwise we'd find "Collection" for "collection" etc
                    return null
                }

                return ClassData(nameResolver, classProto)
            }
            catch (e: IOException) {
                throw IllegalStateException(e)
            }

        }
    }
}