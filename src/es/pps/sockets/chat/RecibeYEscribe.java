package es.pps.sockets.chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;


/**
 * Clase que implementa el interfaz runnable para ser ejecutada en otra hebra.
 * 
 * En el constructor recibe un InputStream del que leer l�neas cont�nuamente una
 * vez envuelto en un BufferedReader. Cada vez que lee una l�nea, la env�a a la
 * ventana de chat, recibida en el constructor, a trav�s de su m�todo
 * onTextoIntroducido(). Si se detecta el cierre del stream, se env�a a la
 * ventana de chat un aviso que indica que se ha detectado el cierre del socket
 * por parte del servidor.
 */
class RecibeYEscribe implements Runnable {

	/**
	 * Constructor
	 * 
	 * @param is Stream de entrada del que leer l�neas.
	 */
	RecibeYEscribe(InputStream is, Chat ventanaChat) {

		_reader = new BufferedReader(new InputStreamReader(is));
		_ventanaChat = ventanaChat;

	} // Constructor

	/**
	 * M�todo para ser lanzado en otra hebra. Lee lineas del canal recibido en el
	 * constructor y las escribe por la salida est�ndar.
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

		// Cerramos el canal de entrada. El socket se dar�
		// cuenta, y ver� que el canal de salida est�
		// tambi�n cerrado (por el otro extremo) y
		// dar� error en el pr�ximo intento de escritura.
		try {
			_reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		_ventanaChat.onTextoRecibido("  [El servidor cerr� la entrada]");

	} // run

	BufferedReader _reader;

	Chat _ventanaChat;

} // RecibeYEscribe
