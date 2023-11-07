package com.example.manyToManyBi;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.manyToManyBi.entities.Direccion;
import com.example.manyToManyBi.entities.Usuario;
import com.example.manyToManyBi.repositories.DireccionRepository;
import com.example.manyToManyBi.repositories.UsuarioRepository;

@SpringBootTest
class ManyToManyBiApplicationTests {
	
	@Autowired
	UsuarioRepository usuarioRepository;
	
	@Autowired
	DireccionRepository direccionRepository;

	@Test
	void mainTest() {
		crearUsuarios();
		mostrarTodosUsuariosYsusDirecciones();
		
//		eliminarUsuario();
//		eliminarUsuarioYDireccion();
//		eliminarDireccion();
		eliminarDireccionCompartida();
				
		mostrarTodosUsuariosYsusDirecciones();
		System.out.println("\n--- ESTADO DE LAS TABLAS --- ");
		mostrarTablaDirecciones();
		mostrarTablaUsuarios();
	}
	
	
	void mostrarTodosUsuariosYsusDirecciones() {
		//Recorrido por todos, sin modificar LAZY a EAGER en la relacion 
		System.out.println(" --- mostrarTodosUsuariosYsusDirecciones: Recorrido sin EAGER ---");
		List<Usuario> listUsuarios =(List<Usuario>) usuarioRepository.findAll();		
		for (Usuario u : listUsuarios) {						
			List <Direccion> dirUsuarios =direccionRepository.findByUsuarios(u);
			System.out.println("Nombre: " + u.getNombre() + " . Cantidad de direcciones: "+ dirUsuarios.size());			
			for (Direccion dir : dirUsuarios) {
				System.out.println("\t"+dir.toString());
			}
			
		}
	}
	
	void crearUsuarios() {
		//Dos usuarios, luiky y lidia, luiky con dos direcciones, lidia con dos direcciones y una compartida con luiky la de Caceres.
		/** usuario LUIKY con dos direcciones **/
		Usuario uLuiky = new Usuario();
		uLuiky.setNombre("Luiky");				

		Direccion dirCompartida = new Direccion ();
		dirCompartida.setCalle("Plaza"); dirCompartida.setCiudad("Caceres");dirCompartida.setCodigoPostal(10002);			

		Direccion dirLuiky = new Direccion ();
		dirLuiky.setCalle("Calle"); dirLuiky.setCiudad("Coria");dirLuiky.setCodigoPostal(10800);		

		List<Direccion> direccionesLuiky= new ArrayList<Direccion>();
		direccionesLuiky.add(dirCompartida);
		direccionesLuiky.add(dirLuiky);
		uLuiky.setDirecciones(direccionesLuiky); 										

		/** Usuuario LIDIA con una direccion **/
		Usuario uLidia = new Usuario();
		uLidia.setNombre("Lidia");

		Direccion dirLidia = new Direccion ();
		dirLidia.setCalle("Carrer"); dirLidia.setCiudad("Sabadell"); dirLidia.setCodigoPostal(28756);

		List<Direccion> direccionesLidia= new ArrayList<Direccion>();
		direccionesLidia.add(dirLidia);		
		direccionesLidia.add(dirCompartida);		
		uLidia.setDirecciones(direccionesLidia);

		//para sincronizar la memoria puedo add los usuarios a las direcciones				
		dirLuiky.getUsuarios().add(uLuiky); //d1 es de luiky. No hago las demás, es un ejemplo al recuperarlo de la BD, todo cuadrará
		
		dirCompartida.getUsuarios().add(uLidia);
		dirCompartida.getUsuarios().add(uLuiky);
		
		dirLidia.getUsuarios().add(uLidia);

		//persisto usuario y sus direcciones. SIN CASCADE PERSIST
		dirLuiky = direccionRepository.save(dirLuiky);
		dirLidia = direccionRepository.save(dirLidia);
		dirCompartida = direccionRepository.save(dirCompartida);
		

		uLuiky=usuarioRepository.save(uLuiky);
		uLidia=usuarioRepository.save(uLidia);
		
		//Si la relacion, en Usuario, es con cascade persist y merge solo el usuario, pero usando saveAll para evitar detached exception de la dirCompartida
//		usuarioRepository.saveAll(List.of(uLuiky,uLidia));


		System.out.println("\tDos usuarios, luiky y lidia, luiky con dos direcciones: \n\tlidia con dos direcciones y una compartida con luiky la de Caceres: Persistidos.\n");
	}
	
	/*
	 * Sub Test 01: Eliminamos el padre un usario a lo BRUTO conozco el ID.
	 * 
	 * Hibernate se encarga de borrar en la joinTable las filas que tenian la id del padre (usuario).
	 * No borra las entidades hijas que tenían ese padre 
	*/ 
	void eliminarUsuario() {
		Usuario u =usuarioRepository.findById(1L).get();
		usuarioRepository.delete(u);
	}
	void eliminarUsuarioYDireccion() {
		System.out.println("-- Eliminar Usuario --");
		Usuario u =usuarioRepository.findById(1L).get();
		u.setDirecciones(direccionRepository.findByUsuarios(u)); //lazy
		List <Direccion> direccionesUnicas = usuarioRepository.encuentraDireccionesUnicasByUsuario(u);		
		
		for (Direccion direccion : direccionesUnicas) {
			System.out.println("\t Eliminar: "+ direccion.toString());
						
			u.getDirecciones().remove(direccion);
			u=usuarioRepository.save(u);			
			
			direccionRepository.delete(direccion);
		}
		usuarioRepository.delete(u);		
		
	}
	
	/*
	 * Sub Test 02: Eliminamos una direccion a lo BRUTO se el id de la direccion de luiky y la direccion
	 * 
	 */	
	void eliminarDireccion() {
		// Paso 1: Obtén el usuario y la dirección que deseas eliminar
		Usuario u =usuarioRepository.findById(1L).get();		
		Direccion d = direccionRepository.findById(1L).get(); //primera direccion Coria, no compartida
		
		// Paso 2: Elimina la dirección de la colección del usuario.
		u.setDirecciones(direccionRepository.findByUsuarios(u)); //esta en lazy necesito cargarles las direcciones
		u.getDirecciones().remove(d); //una vez borrada de la coleccion me va a permitir borrarla del repo. Sino integral reference exception
		u=usuarioRepository.save(u);
		
		// Paso 3: Elimina la dirección de la base de datos. 
		//Al no incluir Cascade Persist debo borrar explicitamente la dir del repo, para que la BD quede correcta y se elimine tb la direccion
		//Eliminar la direccion o extendiendo, ese lado de la relacion, puede considerarse una decision de implementacion. 
		//En una relacion N a M puede que tenga sentido tener registros (entidades) no asociadas. 
		//Imaginad otro caso usuarios y direcciones. Direcciones de una ciudad, es n a m, ¿puede una casa(direccion) estar vacia?		
		direccionRepository.delete(d);
	}
	
	/*
	 * Sub Test 03: Eliminamos una direccion a lo BRUTO se el id de la direccion de luiky y la direccion compartida con Lidia.
	 *      Se borra esa direccion de luiky. Se borra de la jointable. Pero permanece en la BD y permanece relacionada para Lidia. 
	 * 
	 */	
	void eliminarDireccionCompartida() {
		// Paso 1: Obtén el usuario y la dirección que deseas eliminar
		Usuario u =usuarioRepository.findById(1L).get();		
		Direccion d = direccionRepository.findById(3L).get(); //dir Compartida
		
		// Paso 2: Elimina la dirección de la colección del usuario.
		u.setDirecciones(direccionRepository.findByUsuarios(u)); //esta en LAZY necesito cargarles las direcciones
		u.getDirecciones().remove(d); //Borrada de ese usuario 
		u=usuarioRepository.save(u); //usuario guardado y relacion usuario__id y direccion _id en join table eliminada 
		
	}

	void mostrarTablaDirecciones () {
		System.out.println("-- mostrar Tabla Direcciones --");
		for (Direccion d : direccionRepository.findAll()) {
			System.out.println("\t" +d.toString());
		}
	}
	
	void mostrarTablaUsuarios () {
		System.out.println("-- USUARIOS mostrar Tabla USUARIOS --");
		for (Usuario u : usuarioRepository.findAll()) {
			System.out.println("\t" +u.toString());
		}
	}
}
