package es.pps.sockets.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


/**
 * Clase que implementa el interfaz runnable para ser ejecutada en otra hebra.
 * 
 * En el constructor recibe un InputStream del que leer líneas contínuamente una
 * vez envuelto en un BufferedReader. Cada vez que lee una línea, la envía a la
 * ventana de chat, recibida en el constructor, a través de su método
 * onTextoIntroducido(). Si se detecta el cierre del stream, se envía a la
 * ventana de chat un aviso que indica que se ha detectado el cierre del socket
 * por parte del servidor.
 */
class RecibeYEscribe implements Runnable {

	/**
	 * Constructor
	 * 
	 * @param is Stream de entrada del que leer líneas.
	 */
	RecibeYEscribe(InputStream is, Chat ventanaChat) {

		_reader = new BufferedReader(new InputStreamReader(is));
		_ventanaChat = ventanaChat;

	} // Constructor

	/**
	 * Método para ser lanzado en otra hebra. Lee lineas del canal recibido en el
	 * constructor y las escribe por la salida estándar.
	 */
	@Override
	public void run() {

		String leido;

		while (true) {
			try {
				leido = _reader.readLine();
			} catch (IOException e) {
				break;
			}
			if (leido == null) {
				// EOF.
				break;
			}
			_ventanaChat.onTextoRecibido(leido + "\n");
		} // while

		// Cerramos el canal de entrada. El socket se dará
		// cuenta, y verá que el canal de salida está
		// también cerrado (por el otro extremo) y
		// dará error en el próximo intento de escritura.
		try {
			_reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		_ventanaChat.onTextoRecibido("  [El servidor cerró la entrada]");

	} // run

	BufferedReader _reader;

	Chat _ventanaChat;

} // RecibeYEscribe
