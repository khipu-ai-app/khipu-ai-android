package pe.khipuai.app.core.network

import pe.khipuai.app.data.remote.dto.DuplicateNoteInfo

/**
 * T-17: excepción tipada que se lanza cuando el backend rechaza un
 * upload con HTTP 409 + `code: "duplicate"`. A diferencia de
 * [KhipuNetworkException], lleva metadata estructurada (id y título de
 * la nota duplicada) para que la UI pueda mostrar el dialog
 * correspondiente sin tener que re-parsear el body crudo.
 *
 * Esta excepción NO es un error de red ni de servidor: es un caso de
 * uso esperado, así que los ViewModels la manejan por separado (no
 * muestran el Snackbar genérico de "Algo salió mal").
 */
class DuplicateNoteException(
    val info: DuplicateNoteInfo,
) : Exception("Documento duplicado: '${info.existingNoteTitle}'")
