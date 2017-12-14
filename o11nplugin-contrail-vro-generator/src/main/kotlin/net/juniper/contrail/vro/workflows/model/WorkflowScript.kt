/*
 * Copyright (c) 2018 Juniper Networks, Inc. All rights reserved.
 */

package net.juniper.contrail.vro.workflows.model

import net.juniper.contrail.vro.generator.CDATA
import javax.xml.bind.annotation.XmlAccessType
import javax.xml.bind.annotation.XmlAccessorType
import javax.xml.bind.annotation.XmlAttribute
import javax.xml.bind.annotation.XmlType
import javax.xml.bind.annotation.XmlValue

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "scriptType",
    propOrder = arrayOf("value")
)
class WorkflowScript (
    encoded: Boolean = false,
    value: String? = null
) {
    @XmlAttribute(name = "encoded")
    val encoded: String? = encoded.toString()

    @XmlValue
    val value: String? = value.CDATA
}
