package com.example.myapplication

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerLayoutType
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private const val LONGITUD_MATRICULA = 7
private const val MAX_TEXTO = 60

// Ejemplos que se le muestran al usuario cuando el dato no es válido
private const val EJEMPLO_NOMBRE = "María López Hernández"
private const val EJEMPLO_ASIGNATURA = "Programación Móvil"
private val EJEMPLO_MATRICULA = (1..LONGITUD_MATRICULA).joinToString("") { (it % 10).toString() }

data class Registro(
    val nombre: String,
    val matricula: String,
    val asignatura: String,
    val hora: String,
    val fechaEntrega: String
)

/** Un dato que no es válido, para mostrarlo en la alerta. */
private data class Problema(val campo: String, val mensaje: String, val ejemplo: String? = null)

// Permite que la card no se pierda al girar la pantalla
private val RegistroSaver = listSaver<Registro?, String>(
    save = { r ->
        if (r == null) emptyList()
        else listOf(r.nombre, r.matricula, r.asignatura, r.hora, r.fechaEntrega)
    },
    restore = { d -> if (d.size == 5) Registro(d[0], d[1], d[2], d[3], d[4]) else null }
)


private fun validarNombre(v: String, completo: Boolean): String? = when {
    v.any { !it.isLetter() && it != ' ' } -> "Solo se permiten letras y espacios, sin números ni símbolos"
    v.length > MAX_TEXTO -> "Máximo $MAX_TEXTO caracteres"
    !completo -> null
    v.isBlank() -> "Escribe tu nombre"
    v.trim().length < 3 -> "Escribe al menos 3 letras"
    else -> null
}

private fun validarMatricula(v: String, completo: Boolean): String? = when {
    v.any { it !in '0'..'9' } -> "Solo se permiten números, sin letras, espacios ni guiones"
    v.length > LONGITUD_MATRICULA -> "Tiene más de $LONGITUD_MATRICULA dígitos"
    !completo -> null
    v.isEmpty() -> "Escribe tu matrícula"
    v.length < LONGITUD_MATRICULA -> "Faltan dígitos: debe tener $LONGITUD_MATRICULA (llevas ${v.length})"
    else -> null
}

private fun validarAsignatura(v: String, completo: Boolean): String? = when {
    v.any { !it.isLetterOrDigit() && it != ' ' } -> "Solo se permiten letras, números y espacios"
    v.isNotBlank() && v.none { it.isLetter() } -> "Escribe el nombre de la materia, no solo números"
    v.length > MAX_TEXTO -> "Máximo $MAX_TEXTO caracteres"
    !completo -> null
    v.isBlank() -> "Escribe la asignatura"
    v.trim().length < 3 -> "Escribe al menos 3 caracteres"
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    // rememberSaveable: los datos sobreviven a la rotación de pantalla
    var nombre by rememberSaveable { mutableStateOf("") }
    var matricula by rememberSaveable { mutableStateOf("") }
    var asignatura by rememberSaveable { mutableStateOf("") }
    var hora by rememberSaveable { mutableStateOf("") }
    var fechaMillis by rememberSaveable { mutableStateOf<Long?>(null) }

    var intentoGuardar by rememberSaveable { mutableStateOf(false) }
    var mostrarAlerta by rememberSaveable { mutableStateOf(false) }
    var mostrarReloj by rememberSaveable { mutableStateOf(false) }
    var mostrarCalendario by rememberSaveable { mutableStateOf(false) }
    var registro by rememberSaveable(stateSaver = RegistroSaver) { mutableStateOf<Registro?>(null) }

    val focusManager = LocalFocusManager.current

    // Errores que se ven debajo de cada campo:
    // los de formato aparecen al instante; los de "vacío/incompleto" después de Guardar
    val errNombre = validarNombre(nombre, completo = intentoGuardar)
    val errMatricula = validarMatricula(matricula, completo = intentoGuardar)
    val errAsignatura = validarAsignatura(asignatura, completo = intentoGuardar)
    val errHora = if (intentoGuardar && hora.isEmpty()) "Selecciona la hora" else null
    val errFecha = if (intentoGuardar && fechaMillis == null) "Selecciona la fecha" else null

    // Lista completa de problemas para la alerta al presionar Guardar
    val problemas = listOfNotNull(
        validarNombre(nombre, completo = true)?.let { Problema("Nombre", it, EJEMPLO_NOMBRE) },
        validarMatricula(matricula, completo = true)?.let { Problema("Matrícula", it, EJEMPLO_MATRICULA) },
        validarAsignatura(asignatura, completo = true)?.let { Problema("Asignatura", it, EJEMPLO_ASIGNATURA) },
        if (hora.isEmpty()) Problema("Hora que se imparte", "Selecciona la hora") else null,
        if (fechaMillis == null) Problema("Fecha de entrega", "Selecciona la fecha") else null
    )

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                // Tablet o celular en horizontal: campos en dos columnas
                val pantallaAncha = maxWidth >= 600.dp
                // Celular en horizontal: los selectores usan su versión compacta
                val pantallaBaja = maxHeight < 480.dp

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding() // respeta barra de estado, notch y teclado
                ) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .widthIn(max = 720.dp) // en tablets no se estira de más
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Registro de entrega",
                            style = MaterialTheme.typography.headlineSmall
                        )

                        // ---------- Nombre y Matrícula ----------
                        DosColumnas(
                            ancha = pantallaAncha,
                            primero = { mod ->
                                OutlinedTextField(
                                    value = nombre,
                                    onValueChange = { nombre = it }, // acepta cualquier texto
                                    label = { Text("Nombre") },
                                    singleLine = true,
                                    isError = errNombre != null,
                                    trailingIcon = iconoAlerta(errNombre),
                                    supportingText = textoError(errNombre, EJEMPLO_NOMBRE),
                                    keyboardOptions = KeyboardOptions(
                                        capitalization = KeyboardCapitalization.Words,
                                        keyboardType = KeyboardType.Text,
                                        imeAction = ImeAction.Next
                                    ),
                                    modifier = mod
                                )
                            },
                            segundo = { mod ->
                                OutlinedTextField(
                                    value = matricula,
                                    onValueChange = { matricula = it }, // acepta cualquier texto
                                    label = { Text("Matrícula") },
                                    singleLine = true,
                                    isError = errMatricula != null,
                                    trailingIcon = iconoAlerta(errMatricula),
                                    supportingText = {
                                        if (errMatricula != null) {
                                            MensajeConEjemplo(errMatricula, EJEMPLO_MATRICULA)
                                        } else {
                                            Text("${matricula.length}/$LONGITUD_MATRICULA")
                                        }
                                    },
                                    keyboardOptions = KeyboardOptions(
                                        // Teclado normal para que se pueda escribir cualquier cosa
                                        keyboardType = KeyboardType.Text,
                                        imeAction = ImeAction.Next
                                    ),
                                    modifier = mod
                                )
                            }
                        )

                        // ---------- Asignatura ----------
                        OutlinedTextField(
                            value = asignatura,
                            onValueChange = { asignatura = it }, // acepta cualquier texto
                            label = { Text("Asignatura") },
                            singleLine = true,
                            isError = errAsignatura != null,
                            trailingIcon = iconoAlerta(errAsignatura),
                            supportingText = textoError(errAsignatura, EJEMPLO_ASIGNATURA),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Sentences,
                                keyboardType = KeyboardType.Text,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // ---------- Hora y Fecha (se eligen con selectores) ----------
                        DosColumnas(
                            ancha = pantallaAncha,
                            primero = { mod ->
                                CampoSeleccion(
                                    valor = hora,
                                    etiqueta = "Hora que se imparte",
                                    icono = "🕒",
                                    error = errHora,
                                    onClick = {
                                        focusManager.clearFocus()
                                        mostrarReloj = true
                                    },
                                    modifier = mod
                                )
                            },
                            segundo = { mod ->
                                CampoSeleccion(
                                    valor = fechaMillis?.let { formatearFecha(it) } ?: "",
                                    etiqueta = "Fecha de entrega",
                                    icono = "📅",
                                    error = errFecha,
                                    onClick = {
                                        focusManager.clearFocus()
                                        mostrarCalendario = true
                                    },
                                    modifier = mod
                                )
                            }
                        )

                        // ---------- Botón Guardar ----------
                        Button(
                            onClick = {
                                intentoGuardar = true
                                focusManager.clearFocus()
                                val fecha = fechaMillis
                                if (problemas.isEmpty() && fecha != null) {
                                    registro = Registro(
                                        nombre = nombre.trim(),
                                        matricula = matricula,
                                        asignatura = asignatura.trim(),
                                        hora = hora,
                                        fechaEntrega = formatearFecha(fecha)
                                    )
                                    // Limpia el formulario para capturar otro registro
                                    nombre = ""
                                    matricula = ""
                                    asignatura = ""
                                    hora = ""
                                    fechaMillis = null
                                    intentoGuardar = false
                                } else {
                                    // Hay datos no válidos: se muestra la alerta
                                    mostrarAlerta = true
                                }
                            },
                            modifier = if (pantallaAncha) {
                                Modifier.align(Alignment.End).widthIn(min = 220.dp)
                            } else {
                                Modifier.fillMaxWidth()
                            }
                        ) {
                            Text("Guardar")
                        }

                        // ---------- Card temporal debajo del botón ----------
                        registro?.let { r ->
                            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Datos guardados",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    HorizontalDivider()
                                    FilaDato("Nombre", r.nombre)
                                    FilaDato("Matrícula", r.matricula)
                                    FilaDato("Asignatura", r.asignatura)
                                    FilaDato("Hora", r.hora)
                                    FilaDato("Entrega", r.fechaEntrega)
                                }
                            }
                        }
                    }
                }

                // ---------- Alerta de datos no válidos ----------
                if (mostrarAlerta && problemas.isNotEmpty()) {
                    AlertDialog(
                        onDismissRequest = { mostrarAlerta = false },
                        title = { Text("Datos no válidos") },
                        text = {
                            Column(
                                modifier = Modifier.verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text("Corrige lo siguiente antes de guardar:")
                                problemas.forEach { p ->
                                    Column {
                                        Text(p.campo, fontWeight = FontWeight.SemiBold)
                                        Text(p.mensaje, color = MaterialTheme.colorScheme.error)
                                        if (p.ejemplo != null) {
                                            Text("Ejemplo: ${p.ejemplo}")
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { mostrarAlerta = false }) { Text("Corregir") }
                        }
                    )
                }

                // ---------- Diálogo de hora ----------
                if (mostrarReloj) {
                    val partes = hora.split(":").mapNotNull { it.toIntOrNull() }
                    val estadoHora = rememberTimePickerState(
                        initialHour = partes.getOrNull(0) ?: 7,
                        initialMinute = partes.getOrNull(1) ?: 0,
                        is24Hour = true
                    )
                    AlertDialog(
                        onDismissRequest = { mostrarReloj = false },
                        title = { Text("Hora de la clase") },
                        text = {
                            // En horizontal el reloj no cabe: se usa la entrada con teclado
                            if (pantallaBaja) {
                                TimeInput(state = estadoHora)
                            } else {
                                TimePicker(
                                    state = estadoHora,
                                    layoutType = TimePickerLayoutType.Vertical
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                hora = "${dos(estadoHora.hour)}:${dos(estadoHora.minute)}"
                                mostrarReloj = false
                            }) { Text("Aceptar") }
                        },
                        dismissButton = {
                            TextButton(onClick = { mostrarReloj = false }) { Text("Cancelar") }
                        }
                    )
                }

                // ---------- Diálogo de fecha ----------
                if (mostrarCalendario) {
                    val estadoFecha = rememberDatePickerState(
                        initialSelectedDateMillis = fechaMillis,
                        initialDisplayMode = if (pantallaBaja) DisplayMode.Input else DisplayMode.Picker
                    )
                    DatePickerDialog(
                        onDismissRequest = { mostrarCalendario = false },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    fechaMillis = estadoFecha.selectedDateMillis
                                    mostrarCalendario = false
                                },
                                enabled = estadoFecha.selectedDateMillis != null
                            ) { Text("Aceptar") }
                        },
                        dismissButton = {
                            TextButton(onClick = { mostrarCalendario = false }) { Text("Cancelar") }
                        }
                    ) {
                        DatePicker(state = estadoFecha)
                    }
                }
            }
        }
    }
}

@Composable
private fun DosColumnas(
    ancha: Boolean,
    primero: @Composable (Modifier) -> Unit,
    segundo: @Composable (Modifier) -> Unit
) {
    if (ancha) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            primero(Modifier.weight(1f))
            segundo(Modifier.weight(1f))
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            primero(Modifier.fillMaxWidth())
            segundo(Modifier.fillMaxWidth())
        }
    }
}

/** Campo de solo lectura que abre un selector al tocarlo (para hora y fecha). */
@Composable
private fun CampoSeleccion(
    valor: String,
    etiqueta: String,
    icono: String,
    error: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        OutlinedTextField(
            value = valor,
            onValueChange = {},
            readOnly = true,
            label = { Text(etiqueta) },
            trailingIcon = { Text(icono) },
            singleLine = true,
            isError = error != null,
            supportingText = textoError(error),
            modifier = Modifier.fillMaxWidth()
        )
        // Capa transparente encima del campo para detectar el toque
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(onClickLabel = "Seleccionar $etiqueta", onClick = onClick)
        )
    }
}

@Composable
private fun FilaDato(etiqueta: String, valor: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "$etiqueta:",
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(110.dp)
        )
        Text(text = valor, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MensajeConEjemplo(mensaje: String, ejemplo: String?) {
    Column {
        Text(mensaje)
        if (ejemplo != null) {
            Text("Ejemplo: $ejemplo")
        }
    }
}

private fun textoError(mensaje: String?, ejemplo: String? = null): (@Composable () -> Unit)? {
    if (mensaje == null) return null
    return { MensajeConEjemplo(mensaje, ejemplo) }
}

private fun iconoAlerta(error: String?): (@Composable () -> Unit)? {
    if (error == null) return null
    return { Text("⚠️") }
}

private fun dos(n: Int): String = n.toString().padStart(2, '0')

private fun esBisiesto(anio: Int) = (anio % 4 == 0 && anio % 100 != 0) || anio % 400 == 0


private fun formatearFecha(millis: Long): String {
    var dias = (millis / 86_400_000L).toInt() // días desde el 01/01/1970
    var anio = 1970
    while (dias >= (if (esBisiesto(anio)) 366 else 365)) {
        dias -= if (esBisiesto(anio)) 366 else 365
        anio++
    }
    val diasPorMes = intArrayOf(31, if (esBisiesto(anio)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
    var mes = 0
    while (dias >= diasPorMes[mes]) {
        dias -= diasPorMes[mes]
        mes++
    }
    return "${dos(dias + 1)}/${dos(mes + 1)}/$anio"
}