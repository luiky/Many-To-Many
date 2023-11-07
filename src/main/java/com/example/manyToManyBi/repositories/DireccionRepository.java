package com.example.manyToManyBi.repositories;

import java.util.List;

import org.springframework.data.repository.CrudRepository;
import com.example.manyToManyBi.entities.Direccion;
import com.example.manyToManyBi.entities.Usuario;

public interface DireccionRepository extends CrudRepository<Direccion, Long> {
	
    List<Direccion> findByUsuarios(Usuario usuario);
}
