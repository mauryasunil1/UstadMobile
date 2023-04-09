package com.ustadmobile.view.components

import js.core.jso
import mui.material.*
import react.*


external interface UstadSelectFieldProps<T: Any>: Props {

    var options: List<T>

    var itemValue: (T) -> String

    var itemLabel: (T) -> ReactNode

    var onChange: (T) -> Unit

    var label: String

    var id: String

    var value: String

    var fullWidth: Boolean

    var enabled: Boolean?

    var error: String?

}

private val UstadSelectFieldFC = FC<UstadSelectFieldProps<Any>> { props ->

    FormControl {
        fullWidth = true

        InputLabel {
            id = "${props.id}_label"
            +props.label
        }

        Select {
            value = props.value
            id = props.id
            labelId = "${props.id}_label"
            label = ReactNode(props.label)
            disabled = !(props.enabled ?: true)
            fullWidth = props.fullWidth
            onChange = { event, _ ->
                val selectedVal = ("" + event.target.value)
                val selectedItem = props.options.first { props.itemValue(it) ==  selectedVal }
                props.onChange(selectedItem)
            }

            props.options.forEach { option ->
                MenuItem {
                    value = props.itemValue(option)
                    + props.itemLabel(option)
                }
            }
        }

        val helperText = props.error
        if(helperText != null) {
            FormHelperText {
                +helperText
            }
        }
    }

}

/**
 * As per :
 * https://github.com/JetBrains/kotlin-wrappers/blob/c64e209ff5ffc35c2c1fa0d94a259b24ac848123/kotlin-react-select/src/main/kotlin/react/select/Select.ext.kt#L7
 */
fun <Option: Any> ChildrenBuilder.UstadSelectField(
    block: @ReactDsl  UstadSelectFieldProps<Option>.() -> Unit
) {
    child(UstadSelectFieldFC, jso(block))
}
