package com.bancocpm.atm.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.bancocpm.atm.entity.Cliente;
import com.bancocpm.atm.repository.CuentaRepository;
import com.bancocpm.atm.services.ClienteService;
import com.bancocpm.atm.services.CuentaService;
import com.bancocpm.atm.services.MovimientoService;
import com.bancocpm.atm.services.RetiroService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
@RequestMapping("/cajero")
public class CajeroController {
    private final ClienteService clienteService;
    private final CuentaService cuentaService;
    private final CuentaRepository cuentaRepository;
    private final MovimientoService movimientoService;
    private final RetiroService retiroService;

    @GetMapping
    public String loginForm() {
        return "cajero/login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String numeroCuenta,
            @RequestParam String pin, HttpSession session,
            Model model) {
        var cuenta = cuentaService.buscarPorNumero(numeroCuenta);
        if (cuenta.isEmpty()) {
            model.addAttribute("error", "Cuenta no se encuentra o Inexistente");
            return "cajero/login";
        }

        Cliente cliente = cuenta.get().getCliente();

        if (cliente.isBloqueado()) {
            model.addAttribute("error", "Cuenta Bloqueada");
            return "cajero/login";
        }

        if (!cliente.getPin().equals(pin)) {
            clienteService.incrementarIntento(cliente);
            if (cliente.getIntentos() >= 3) {
                clienteService.bloquearCliente(cliente);
                model.addAttribute("error", "Cuenta Bloqueada por intentos fallidos");

            } else {
                model.addAttribute("error", "Pin Incorrecto");
            }
            return "cajero/login";
        }

        clienteService.reiniciarIntentos(cliente);
        session.setAttribute("cliente", cliente);
        return "redirect:/cajero/menu";
    }

}