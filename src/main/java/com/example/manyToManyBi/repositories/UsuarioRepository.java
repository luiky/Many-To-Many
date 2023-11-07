package com.example.manyToManyBi.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import com.example.manyToManyBi.entities.Direccion;
import com.example.manyToManyBi.entities.Usuario;

public interface UsuarioRepository extends CrudRepository<Usuario, Long> {

	Optional<Usuario> findByNombre(String n);
	
    @Query("SELECT d FROM Direccion d " +
            "WHERE d NOT IN (SELECT DISTINCT d FROM Usuario u " +
            "JOIN u.direcciones d WHERE u != :usuario)")
     List<Direccion> encuentraDireccionesUnicasByUsuario(@Param("usuario") Usuario usuario);
}

