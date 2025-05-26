package com.bancocpm.atm.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    // iniciar sesión
    @PostMapping("/login")
    public String login(@RequestParam String numeroCuenta, @RequestParam String pin, HttpSession session, Model model) {
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

    @GetMapping("/menu")
    public String menu(HttpSession session, Model model) {
        Cliente cliente = (Cliente) session.getAttribute("cliente");
        if (cliente == null)
            return "redirect:/cajero";

        model.addAttribute("cliente", cliente);
        model.addAttribute("cuentas", cuentaService.buscarPorCliente(cliente));
        return "cajero/menu";
    }

    @GetMapping("/cosultas")
    public String consultas(Model model, HttpSession session) {
        Cliente cliente = (Cliente) session.getAttribute(name = "cliente");
        model.addAttribute("cuentas", cuentaService.buscarPorCliente(cliente));
        return "cajero/consultas";
    }

    @GetMapping("/movimientos/(numero)")
    public String movimientos(@PathVariable String numero, Model model, HttpSession session) {
        Cliente cliengte = (Cliente) session.getAttribute("cliente");
        if (cliente == null)
            return "redirect:/cajero";

        try {
            var moviemiento = movimientoService.buscarPorCuenta(numero);
            model.addAttribute("movimientos", moviemiento);
            return "cajero/movimientos";
        } catch (Exception e) {
            model.addAttribute("error", "No es posible obtener los movimientos" + e.getMessage());
            return "cajero/consultas";
        }
    }

    // cerrar sesión
    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/cajero";
    }

    @GetMapping("/retiro")
    public String mostrarFormularioRetiro(Model model, HttpSession session) {
        Cliente cliente = (Cliente) session.getAttribute("cliente");
        model.addAttribute("cuentas", cuentaService.buscarPorCliente(cliente));
        return "cajero/retiro";
    }

    @PostMapping("/retiro")
    public String realizarRetiro(@RequestParam String identificacion, @RequestParam String numeroCuenta,
            @RequestParam double monto, RedirectAttributes redirectAttributes) {
        try {
            String resultado = retiroService.realizarRetiro(identificacion, numeroCuenta, monto);
            redirectAttributes.addFlashAttribute("mensaje", "Retiro realizado con éxito: ");
            return resultado;
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al realizar el retiro: " + e.getMessage());
            return "redirect:/cajero/retiro";
        }
    }

    @GetMapping("/consiganr")
    public String mostrarFormularioConsignacion(Model model, HttpSession session) {
        Cliente cliente = (Cliente) session.getAttribute("cliente");
        if (cliente == null) {
            return "redirect:/cajero";
        }
        return "cajero/consignar";
    }

   @PostMapping("/consignar")
    public String consignar(@RequestParam String numeroCuenta, @RequestParam double monto,Model model) {
        try {
            Cuenta cuenta = cuentaRepository.findByNumero(numeroCuenta)
                    .orElseThrow(() -> new RuntimeException("Cuenta no encontrada"));

            movimientoService.realizarConsignacion(cuenta, monto);
            model.addAttribute("mensaje", "Consignación exitosa. Nuevo saldo: " + cuenta.getSaldo());
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }

        return "cajero/consignar";
    }