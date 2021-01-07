package es.pps.sockets.salachat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.List;

/**
 * Clase que implementa el interfaz Runnable para ser lanzada en otra hebra.
 * 
 * Recibe un InputStream como parámetro en el constructor y se queda
 * contínuamente leyendo líneas de él (a través de un BufferedReader). Cada vez
 * que recibe una línea la manda a todos los PrintWriter contenidos en una lista
 * recibida en el constructor, salvo a uno especial que es el asociado al mismo
 * socket que el InputStream de donde estamos leyendo.
 * 
 * @author Pedro Pablo Gómez Martín
 */
class RecibeYEscribe implements Runnable {
	
	BufferedReader _reader;
	PrintWriter _pw;
	List<PrintWriter> _writers;
	
	/**
	 * Constructor
	 * 
	 * @param is      Stream de entrada del que leer líneas.
	 * @param pw      PrintWriter asociado al mismo socket que el stream del primer
	 *                parámetro.
	 * @param writers Lista con los canales de salida de todos los clientes.
	 */
	RecibeYEscribe(InputStream is, PrintWriter pw, List<PrintWriter> writers) {

		_reader = new BufferedReader(new InputStreamReader(is));
		_pw = pw;
		_writers = writers;

	} // Constructor

	/**
	 * Método para ser lanzado en otra hebra. Lee lineas del canal recibido en el
	 * constructor y las escribe por todos los PrintWriter de la lista recibida en
	 * el constructor.
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
			for (PrintWriter cliente : _writers) {
				if (cliente != _pw) {
					// El PrintWriter siguiente no es el cliente al que
					// estamos escuchando aquí (para no mandarnos a
					// nosotros mismos lo que acabamos de enviar)
					cliente.println(leido);
					if (cliente.checkError())
						System.err.println("\t[Error en el último envío]");
				}
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

		System.out.println("  [Fin de la hebra de entrada]");

	} // run

	

} // class RecibeYReenvia

//--------------------------------------------------------
//--------------------------------------------------------
