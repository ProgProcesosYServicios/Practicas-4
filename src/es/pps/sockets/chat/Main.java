package es.pps.sockets.chat;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

public class Main {

	/**
	 * Programa principal. Interpreta los parámetros crea el socket con el servidor
	 * y lanza la ventana de chat.
	 * 
	 * @param args Parámetros en la línea de órdenes. El primero debe ser el nombre
	 *             del servidor, el segundo el puerto, y el tercero el nombre con el
	 *             que se conocerá al usuario en el chat.
	 */
	public static void main(String[] args) {

	
		Socket socket1, socket2;
		String hostname = "localhost";
		int port = 4567;

		try {
			socket1 = new Socket(hostname, port);
			socket2 = new Socket(hostname, port);
		} catch (UnknownHostException uhe) {
			System.err.println("No se pudo resolver " + hostname);
			return;
		} catch (IOException ioe) {
			System.err.println("Error de E/S al crear el socket: " + ioe.getLocalizedMessage());
			return;
		}

		new Chat(socket1, "bob");
		new Chat(socket2, "alice");
	} // main

}
