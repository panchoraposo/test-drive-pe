package com.neuralbank.api;

import com.neuralbank.dto.request.CreateCustomerRequest;
import com.neuralbank.dto.request.UpdateCustomerRequest;
import com.neuralbank.dto.response.CreditScoreResponse;
import com.neuralbank.dto.response.CustomerResponse;
import com.neuralbank.dto.response.PageResponse;
import com.neuralbank.enums.CustomerType;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * Customer API consumed by customer-service-mcp ({@code CustomerClient} at {@code /api/customers}).
 */
@Path("/api/customers")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CustomerResource {

    private static final AtomicLong ID_SEQ = new AtomicLong(3);
    private static final ConcurrentMap<Long, CustomerResponse> CUSTOMERS = new ConcurrentHashMap<>();

    static {
        seed(1L, "12345678-9", "RUT", "Juan", "Pérez", "juan@neuralbank.com", "+56911111111",
                "Santiago", 1L, CustomerType.PERSONAL, new BigDecimal("750"), "Bajo", true);
        seed(2L, "98765432-1", "RUT", "María", "García", "maria@neuralbank.com", "+56922222222",
                "Valparaíso", 1L, CustomerType.PERSONAL, new BigDecimal("820"), "Bajo", true);
        seed(3L, "11223344-5", "RUT", "Carlos", "López", "carlos@neuralbank.com", "+56933333333",
                "Concepción", 1L, CustomerType.EMPRESARIAL, new BigDecimal("680"), "Medio", true);
    }

    private static void seed(Long id, String identificacion, String tipoIdentificacion, String nombre,
                             String apellido, String email, String telefono, String ciudad, Long paisId,
                             CustomerType tipoCliente, BigDecimal score, String nivelRiesgo, boolean activo) {
        CustomerResponse c = new CustomerResponse();
        c.id = id;
        c.identificacion = identificacion;
        c.tipoIdentificacion = tipoIdentificacion;
        c.nombre = nombre;
        c.apellido = apellido;
        c.nombreCompleto = nombre + " " + apellido;
        c.email = email;
        c.telefono = telefono;
        c.ciudad = ciudad;
        c.paisId = paisId;
        c.tipoCliente = tipoCliente;
        c.scoreCrediticio = score;
        c.nivelRiesgo = nivelRiesgo;
        c.activo = activo;
        c.fechaRegistro = LocalDate.now().minusYears(1);
        c.createdAt = LocalDateTime.now().minusMonths(6);
        c.updatedAt = c.createdAt;
        CUSTOMERS.put(id, c);
    }

    @POST
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        if (request == null || request.identificacion == null || request.nombre == null) {
            throw new BadRequestException("identificacion and nombre are required");
        }
        boolean duplicate = CUSTOMERS.values().stream()
                .anyMatch(c -> request.identificacion.equals(c.identificacion));
        if (duplicate) {
            throw new WebApplicationException("Customer already exists", 409);
        }

        long id = ID_SEQ.incrementAndGet();
        CustomerResponse c = new CustomerResponse();
        c.id = id;
        c.identificacion = request.identificacion;
        c.tipoIdentificacion = request.tipoIdentificacion != null ? request.tipoIdentificacion : "RUT";
        c.nombre = request.nombre;
        c.apellido = request.apellido != null ? request.apellido : "";
        c.nombreCompleto = c.nombre + " " + c.apellido;
        c.email = request.email;
        c.telefono = request.telefono;
        c.direccion = request.direccion;
        c.ciudad = request.ciudad;
        c.estadoProvincia = request.estadoProvincia;
        c.codigoPostal = request.codigoPostal;
        c.paisId = request.paisId != null ? request.paisId : 1L;
        c.tipoCliente = request.tipoCliente != null ? request.tipoCliente : CustomerType.PERSONAL;
        c.scoreCrediticio = request.scoreCrediticio != null ? request.scoreCrediticio : new BigDecimal("700");
        c.nivelRiesgo = riskFromScore(c.scoreCrediticio);
        c.activo = true;
        c.fechaRegistro = LocalDate.now();
        c.createdAt = LocalDateTime.now();
        c.updatedAt = c.createdAt;
        CUSTOMERS.put(id, c);
        return c;
    }

    @GET
    @Path("/{customerId}")
    public CustomerResponse getCustomerById(@PathParam("customerId") Long customerId) {
        CustomerResponse c = CUSTOMERS.get(customerId);
        if (c == null) {
            throw new NotFoundException("Customer not found: " + customerId);
        }
        return c;
    }

    @GET
    @Path("/identification/{identificacion}")
    public CustomerResponse getCustomerByIdentificacion(@PathParam("identificacion") String identificacion) {
        return CUSTOMERS.values().stream()
                .filter(c -> identificacion.equals(c.identificacion))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Customer not found: " + identificacion));
    }

    @PUT
    @Path("/{customerId}")
    public CustomerResponse updateCustomer(@PathParam("customerId") Long customerId, UpdateCustomerRequest request) {
        CustomerResponse c = CUSTOMERS.get(customerId);
        if (c == null) {
            throw new NotFoundException("Customer not found: " + customerId);
        }
        if (request.nombre != null) {
            c.nombre = request.nombre;
        }
        if (request.apellido != null) {
            c.apellido = request.apellido;
        }
        if (request.nombre != null || request.apellido != null) {
            c.nombreCompleto = c.nombre + " " + c.apellido;
        }
        if (request.email != null) {
            c.email = request.email;
        }
        if (request.telefono != null) {
            c.telefono = request.telefono;
        }
        if (request.direccion != null) {
            c.direccion = request.direccion;
        }
        if (request.ciudad != null) {
            c.ciudad = request.ciudad;
        }
        if (request.estadoProvincia != null) {
            c.estadoProvincia = request.estadoProvincia;
        }
        if (request.codigoPostal != null) {
            c.codigoPostal = request.codigoPostal;
        }
        if (request.paisId != null) {
            c.paisId = request.paisId;
        }
        if (request.tipoCliente != null) {
            c.tipoCliente = request.tipoCliente;
        }
        c.updatedAt = LocalDateTime.now();
        return c;
    }

    @GET
    public PageResponse<CustomerResponse> searchCustomers(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("search") String search,
            @QueryParam("tipoCliente") CustomerType tipoCliente,
            @QueryParam("ciudad") String ciudad) {
        List<CustomerResponse> filtered = CUSTOMERS.values().stream()
                .filter(c -> matchesSearch(c, search))
                .filter(c -> tipoCliente == null || tipoCliente == c.tipoCliente)
                .filter(c -> ciudad == null || ciudad.equalsIgnoreCase(c.ciudad))
                .sorted(Comparator.comparing(c -> c.id))
                .collect(Collectors.toList());

        int safeSize = Math.max(1, Math.min(size, 100));
        int total = filtered.size();
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / safeSize);
        int from = Math.min(page * safeSize, total);
        int to = Math.min(from + safeSize, total);
        List<CustomerResponse> pageContent = filtered.subList(from, to);

        PageResponse<CustomerResponse> response = new PageResponse<>();
        response.content = pageContent;
        response.currentPage = page;
        response.totalPages = totalPages;
        response.totalElements = total;
        response.numberOfElements = pageContent.size();
        response.size = safeSize;
        response.first = page == 0;
        response.last = totalPages == 0 || page >= totalPages - 1;
        response.empty = pageContent.isEmpty();
        return response;
    }

    @GET
    @Path("/{customerId}/credit-score")
    public CreditScoreResponse getCreditScore(@PathParam("customerId") Long customerId) {
        CustomerResponse c = requireCustomer(customerId);
        return buildCreditScore(c);
    }

    @POST
    @Path("/{customerId}/credit-score/calculate")
    public CreditScoreResponse calculateCreditScore(@PathParam("customerId") Long customerId) {
        CustomerResponse c = requireCustomer(customerId);
        BigDecimal adjusted = c.scoreCrediticio.add(new BigDecimal("5"));
        if (adjusted.compareTo(new BigDecimal("1000")) > 0) {
            adjusted = new BigDecimal("1000");
        }
        c.scoreCrediticio = adjusted;
        c.nivelRiesgo = riskFromScore(adjusted);
        c.updatedAt = LocalDateTime.now();
        return buildCreditScore(c);
    }

    @POST
    @Path("/{customerId}/activate")
    public void activateCustomer(@PathParam("customerId") Long customerId) {
        CustomerResponse c = requireCustomer(customerId);
        c.activo = true;
        c.updatedAt = LocalDateTime.now();
    }

    @POST
    @Path("/{customerId}/deactivate")
    public void deactivateCustomer(@PathParam("customerId") Long customerId, Map<String, String> body) {
        CustomerResponse c = requireCustomer(customerId);
        c.activo = false;
        c.updatedAt = LocalDateTime.now();
    }

    @PUT
    @Path("/{customerId}/risk-level")
    public void updateRiskLevel(@PathParam("customerId") Long customerId, Map<String, String> body) {
        CustomerResponse c = requireCustomer(customerId);
        if (body != null && body.get("nivelRiesgo") != null) {
            c.nivelRiesgo = body.get("nivelRiesgo");
        }
        c.updatedAt = LocalDateTime.now();
    }

    private static CustomerResponse requireCustomer(Long customerId) {
        CustomerResponse c = CUSTOMERS.get(customerId);
        if (c == null) {
            throw new NotFoundException("Customer not found: " + customerId);
        }
        return c;
    }

    private static boolean matchesSearch(CustomerResponse c, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String q = search.toLowerCase(Locale.ROOT);
        return contains(c.nombre, q) || contains(c.apellido, q) || contains(c.email, q)
                || contains(c.identificacion, q) || contains(c.nombreCompleto, q);
    }

    private static boolean contains(String value, String q) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(q);
    }

    private static CreditScoreResponse buildCreditScore(CustomerResponse c) {
        CreditScoreResponse s = new CreditScoreResponse();
        s.customerId = c.id;
        s.score = c.scoreCrediticio;
        s.nivelRiesgo = c.nivelRiesgo;
        s.evaluacion = evaluationFromScore(c.scoreCrediticio);
        s.calculatedAt = LocalDateTime.now();
        return s;
    }

    private static String riskFromScore(BigDecimal score) {
        int value = score.intValue();
        if (value >= 800) {
            return "Bajo";
        }
        if (value >= 650) {
            return "Medio";
        }
        return "Alto";
    }

    private static String evaluationFromScore(BigDecimal score) {
        int value = score.intValue();
        if (value >= 800) {
            return "Excelente historial crediticio";
        }
        if (value >= 650) {
            return "Historial crediticio aceptable";
        }
        return "Requiere revisión adicional";
    }
}
