package es.pps.sockets.clientefullduplex;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.io.BufferedReader;

/**
 * Clase que implementa el interfaz Runnable para ser
 * lanzado en otra hebra.
 * 
 * Recibe un InputStream como parámetro en el constructor
 * y se queda contínuamente leyendo líneas de él
 * (a través de un BufferedReader). Cada vez que recibe
 * una línea la escribe por la salida estándar. 
 *  
 * @author Pedro Pablo Gómez Martín
 *
 */
class RecibeYEscribe implements Runnable {

	/**
	 * Constructor
	 * 
	 * @param is Stream de entrada del que leer líneas.
	 */
	RecibeYEscribe(InputStream is) {
		_reader = new BufferedReader(new InputStreamReader(is));
	}

	/**
	 * Método para ser lanzado en otra hebra. Lee lineas del canal
	 * recibido en el constructor y las escribe por la salida
	 * estándar.
	 */
	@Override
	public void run() {
		
		String leido;

		while(true) {
			try {
				leido = _reader.readLine();
			} catch (IOException e) {
				break;
			}
			if (leido == null) {
				// EOF.
				break;
			}
			System.out.println(leido);
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

		System.out.println("[Fin de la hebra de entrada]");

	} // run

	BufferedReader _reader;

} // class RecibeYEscribe

//------------------------------------------------
//------------------------------------------------

/**
 * Programa sencillo que se conecta por TCP a un host y puerto
 * especificados como parámetros al programa, y que envía todo lo
 * que se escribe por teclado.
 * 
 * Para atender los datos que llegan desde el otro extremo
 * utiliza una hebra secundaria.
 * 
 * @author Pedro Pablo Gómez Martín
 */
public class ClienteFullDuplex {

	/**
	 * Método auxiliar que recibe una dirección de internet y un
	 * puerto y lo escribe por la salida estándar.
	 * 
	 * @param address Dirección de internet
	 * @param port Puerto
	 */
	protected static void escribeExtremo(InetAddress address, int port) {

		System.out.print(address.getHostAddress());
		System.out.print(":" + port);
		if (address.getCanonicalHostName() != null)
		System.out.print(" (" + address.getCanonicalHostName() + ")");

	} // escribeExtremo

	//------------------------------------------------

	/**
	 * Programa principal.
	 * 
	 * @param args Argumentos en la línea de órdenes. El primero
	 * debe ser el nombre del host al que conectarnos (o la IP) y
	 * el segundo el número de puerto.
	 */
	public static void main(String[] args) {

		Socket socket;
		String hostname;
		int port;

		// Analizamos los parámetros.
		if (args.length < 2) {
			System.err.println("Faltan parámetros: <host> <puerto>");
			return;
		}
		hostname = args[0];
		try {
			port = Integer.parseInt(args[1]);
		}
		catch(NumberFormatException e) {
			System.err.println("Puerto inválido");
			return;
		}

		// Intentamos conectarnos al servidor solicitado.
		try {
			socket = new Socket(hostname, port);
		}
		catch(UnknownHostException uhe) {
			System.err.println("No se pudo resolver " + hostname);
			return;
		}
		catch(IOException ioe) {
			System.err.println("Error de E/S al crear el socket: " + ioe.getLocalizedMessage());
			return;
		}

		// Mostramos información de la conexión.
		System.out.print("Conectado desde ");
		escribeExtremo(socket.getLocalAddress(), socket.getLocalPort());
		System.out.print(" a ");
		escribeExtremo(socket.getInetAddress(), socket.getPort());
		System.out.println();

		// Obtenemos el canal de escritura del socket para mandar texto
		// al servidor.
		PrintWriter out;
		try {
			out = new PrintWriter(socket.getOutputStream());
		} catch (IOException e) {
			System.err.println("No pude conseguir el canal de escritura del socket.");
			try {
				socket.close();
			} catch (IOException ioe) {
			}
			return;
		}

		RecibeYEscribe rye;
		try {
			rye = new RecibeYEscribe(socket.getInputStream());
		} catch (IOException e) {
			System.err.println("No pude conseguir el canal de lectura del socket.");
			try {
				socket.close();
			} catch (IOException ioe) {
			}
			return;
		}
		new Thread(rye).start();

		// Leemos del teclado y mandamos por el socket todo lo
		// que recibimos.
		Scanner scanner = new Scanner(System.in); 
	
		// Damos vueltas mientras el canal de salida esté funcionando y
		// haya más líneas que leer
		while (!out.checkError() && scanner.hasNextLine()) {
			String s = scanner.nextLine();
			out.println(s);
			out.flush(); // Forzamos el envío.
		} // while

		try {
			socket.close();
		} catch (IOException e) {
		}
		scanner.close();

	} // main

} // class ClienteFullDuplex