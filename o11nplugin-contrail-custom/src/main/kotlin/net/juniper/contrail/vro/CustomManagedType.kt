/*
 * Copyright (c) 2018 Juniper Networks, Inc. All rights reserved.
 */

package net.juniper.contrail.vro

import com.vmware.o11n.sdk.modeldriven.extension.Interceptor
import com.vmware.o11n.sdk.modeldrivengen.model.FormalParameter
import com.vmware.o11n.sdk.modeldrivengen.model.ManagedConstructor
import com.vmware.o11n.sdk.modeldrivengen.model.ManagedMethod
import com.vmware.o11n.sdk.modeldrivengen.model.ManagedType
import net.juniper.contrail.vro.config.backRefTypeName
import net.juniper.contrail.vro.config.constants.apiTypesPackageName
import net.juniper.contrail.vro.config.isApiObjectClass
import net.juniper.contrail.vro.config.isApiPropertyClass
import net.juniper.contrail.vro.config.isHiddenProperty
import net.juniper.contrail.vro.config.isInventoryProperty
import net.juniper.contrail.vro.config.isModelClassName
import net.juniper.contrail.vro.config.returnsApiPropertyOrList

class CustomManagedType(private val delegate: ManagedType) : ManagedType() {

    val isObjectClass get() =
        delegate.modelClass?.isApiObjectClass ?: false

    val references: List<CustomReference> = delegate.modelClass?.run {
        declaredMethods.asSequence()
            .map { it.toCustomReference() }.filterNotNull()
            .toList()
    } ?: emptyList()

    val referenceProperties: List<CustomReferenceProperty> = delegate.modelClass?.run {
        declaredFields
            .asSequence()
            .filter { it.name.endsWith("back_refs") }
            .filter { it.backRefTypeName.isModelClassName }
            .map { CustomReferenceProperty.wrapField(it) }
            .toList()
    } ?: emptyList()

    val propertyViews: List<CustomProperty> = delegate.modelClass?.run {
        if (isApiObjectClass)
        methods.asSequence()
            .filter { it.name.startsWith("get") }
            .filter { it.returnsApiPropertyOrList }
            .filter { ! it.returnType.isInventoryProperty }
            .map { it.toCustomProperty() }
            .filter { ! it.propertyName.isHiddenProperty }
            .toList()
        else
            null
    } ?: emptyList()

    init {
        removeDuplicateMethods()
        generateReferenceMethods()
        generatePropertyMethods()
        generateReferencePropertiesMethods()
    }

    private fun removeDuplicateMethods() {
        delegate.methods.asSequence()
            .filter { it.isPropertyEditor }
            .groupBy { it.name }
            .filter { it.value.size > 1 }
            .values
            .forEach { it.removeDuplicates() }
    }

    private fun List<ManagedMethod>.removeDuplicates() {
        val remaining = methodToRetain
        asSequence()
            .filter { it != remaining }
            .forEach { delegate.methods.remove(it) }
    }

    private val List<ManagedMethod>.methodToRetain get() =
        first { it.params.size == 1 && it.params[0].modelType.isApiPropertyClass }

    private val ManagedMethod.isPropertyEditor get() =
        returns.modelType == Void.TYPE && params.let {
            it.size >= 1 && ! it[0].modelType.isApiObjectClass
        }

    private fun generateReferenceMethods() =
        references.forEach { methods.add(it.toManagedMethod()) }

    private fun generateReferencePropertiesMethods() =
        referenceProperties.forEach { methods.add(it.toManagedMethod()) }

    private fun generatePropertyMethods() =
        propertyViews.forEach { methods.add(it.toManagedMethod()) }

    private fun CustomReference.toManagedMethod() = ManagedMethod().apply {
        setName(methodName, methodName)
        params = emptyList()
        // trick to avoid generating standard wrapper method
        setIsInheritedWrapperMethod(true)
        isPropertyReadOnly = true
        returns = collectionReturnFormalParameter()
    }

    private fun CustomReferenceProperty.toManagedMethod() = ManagedMethod().apply {
        val originalProperty = this@toManagedMethod.propertyName

        setName(wrapperMethodName, wrapperMethodName)
        originalPropertyName = originalProperty
        propertyName = originalProperty
        params = emptyList()
        setIsInheritedWrapperMethod(true)
        isPropertyReadOnly = true
        returns = stringReturnFormalParameter()
    }

    private fun CustomProperty.toManagedMethod() = ManagedMethod().apply {
        setName(viewMethodName, viewMethodName)
        propertyName = viewPropertyName
        originalPropertyName = viewPropertyName
        params = emptyList()
        setIsInheritedWrapperMethod(true)
        isPropertyReadOnly = true
        returns = stringReturnFormalParameter()
    }

    private fun CustomReference.collectionReturnFormalParameter() = FormalParameter().apply {
        name = "_result"
        modelType = List::class.java
        fullClassName = List::class.java.name
        typeName = "[Contrail$className"
        componentTypeName = "Contrail$className"
        componentClassName = "$apiTypesPackageName.$className"
        isWrapped = true
    }

    private fun stringReturnFormalParameter() = FormalParameter().apply {
        name = "_result"
        modelType = String::class.java
        fullClassName = String::class.java.name
        typeName = "String"
        isWrapped = false
    }

    companion object {

        fun wrap(type: ManagedType): CustomManagedType {
            return CustomManagedType(type)
        }
    }

    override fun isGenerate(): Boolean =
        delegate.isGenerate

    override fun setGenerate(generate: Boolean) {
        delegate.isGenerate = generate
    }

    override fun isFindable(): Boolean
        = delegate.isFindable

    override fun setFindable(findable: Boolean) {
        delegate.isFindable = findable
    }

    override fun isRootIdPropagated(): Boolean =
        delegate.isRootIdPropagated

    override fun setRootIdPropagated(rootIdPropagated: Boolean) {
        delegate.isRootIdPropagated = rootIdPropagated
    }

    override fun getFullSuperClassName(): String? =
        delegate.fullSuperClassName

    override fun setFullSuperClassName(fullSuperClassName: String) {
        delegate.fullSuperClassName = fullSuperClassName
    }

    override fun getPackageName(): String? =
        delegate.packageName

    override fun getClassName(): String? =
        delegate.className

    override fun getName(): String? =
        delegate.name

    override fun isSingleton(): Boolean =
        delegate.isSingleton

    override fun setSingleton(singeton: Boolean) {
        delegate.isSingleton = singeton
    }

    override fun isCreateable(): Boolean =
        delegate.isCreateable

    override fun setCreateable(createable: Boolean) {
        delegate.isCreateable = createable
    }

    override fun getFullClassName(): String? =
        delegate.fullClassName

    override fun setFullClassName(className: String) {
        delegate.fullClassName = className
    }

    override fun getMethods(): MutableList<ManagedMethod> =
        delegate.methods

    override fun setMethods(methods: List<ManagedMethod>) {
        delegate.methods = methods
    }

    override fun getDoc(): String? =
        delegate.doc

    override fun setDoc(doc: String) {
        delegate.doc = doc
    }

    override fun getConstructors(): List<ManagedConstructor?>? =
        delegate.constructors

    override fun setConstructors(constructors: List<ManagedConstructor>) {
        delegate.constructors = constructors
    }

    override fun toString(): String =
        delegate.toString()

    override fun setName(name: String) {
        delegate.name = name
    }

    override fun getModelClass(): Class<*>? =
        delegate.modelClass

    override fun setModelClass(wrappedClass: Class<*>) {
        delegate.modelClass = wrappedClass
    }

    override fun getInterceptor(): Class<out Interceptor>? =
        delegate.interceptor

    override fun setInterceptor(interceptorClass: Class<out Interceptor>) {
        delegate.interceptor = interceptorClass
    }
}
