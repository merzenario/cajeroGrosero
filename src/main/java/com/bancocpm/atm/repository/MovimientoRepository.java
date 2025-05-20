package com.bancocpm.atm.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bancocpm.atm.entity.Cuenta;
import com.bancocpm.atm.entity.Movimiento;

public interface MovimientoRepository extends JpaRepository<Movimiento, Long>{
    List<Movimiento> findByCuenta(Cuenta cuenta);
    List<Movimiento> findByCuentaOrderByFechaDesc(Cuenta cuenta);

}
