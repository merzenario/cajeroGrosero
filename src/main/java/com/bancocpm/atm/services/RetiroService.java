package com.bancocpm.atm.services;

import org.springframework.stereotype.Service;

import com.bancocpm.atm.entity.Cliente;
import com.bancocpm.atm.entity.Cuenta;
import com.bancocpm.atm.repository.ClienteRepository;
import com.bancocpm.atm.repository.CuentaRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RetiroService {

    private final CuentaRepository cuentaRepository;
    private final ClienteRepository clienteRepository;
    private final MovimientoService movimientoServices;

    public String realizarRetiro(String identificacion, String numeroCuenta, double monto){
        Cliente cliente = clienteRepository.findByIdentificacion(identificacion)
                .orElseThrow(() -> new RuntimeException("Cuenta no encontrada"));

        Cuenta cuenta = cuentaRepository.findByNumero(numeroCuenta)
                .orElseThrow(() -> new RuntimeException("Cuenta no encontrada"));

        if(!cuenta.getCliente().equals(cliente)){
            throw new RuntimeException("La cuenta no pertenece al cliente");
        }

        if(cliente.isBloqueado()){
            throw new RuntimeException("El cliente o su cuenta está bloqueado");
        }

        boolean exito = movimientoServices.realizarRetiro(cuenta, monto);

        if(!exito){
            throw new RuntimeException("Sin saldo suficiente");
        }

        return "redirect:cajero/menu?mensaje=Retiro realizado con éxito";
    }

}
