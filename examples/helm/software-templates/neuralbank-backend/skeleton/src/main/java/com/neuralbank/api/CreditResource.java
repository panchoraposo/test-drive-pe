package com.neuralbank.api;

import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Path("/api")
public class CreditResource {

    private static final ConcurrentMap<String, ConcurrentMap<String, Object>> CREDITS = new ConcurrentHashMap<>();

    static {
        putCredit("CR001", "C001", 25000, "active", "personal");
        putCredit("CR002", "C002", 150000, "active", "mortgage");
        putCredit("CR003", "C003", 8000, "pending", "personal");
    }

    private static void putCredit(String id, String customerId, int amount, String status, String type) {
        ConcurrentMap<String, Object> c = new ConcurrentHashMap<>();
        c.put("id", id);
        c.put("customerId", customerId);
        c.put("amount", amount);
        c.put("status", status);
        c.put("type", type);
        CREDITS.put(id, c);
    }

    @GET
    @Path("/credits")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> listCredits() {
        List<Map<String, Object>> out = new ArrayList<>(CREDITS.values());
        out.sort(Comparator.comparing(m -> String.valueOf(m.getOrDefault("id", ""))));
        return out;
    }

    @POST
    @Path("/credits/{id}/update")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> updateCredit(@PathParam("id") String id, Map<String, Object> body) {
        ConcurrentMap<String, Object> credit = CREDITS.get(id);
        if (credit == null) {
            throw new NotFoundException("Credit not found: " + id);
        }

        String previous = String.valueOf(credit.getOrDefault("status", "unknown"));
        String next = String.valueOf(body.getOrDefault("status", previous));
        String updatedBy = String.valueOf(body.getOrDefault("updatedBy", "system"));

        credit.put("status", next);
        credit.put("updatedBy", updatedBy);
        credit.put("updatedAt", Instant.now().toString());

        return Map.of(
            "creditId", id,
            "previousStatus", previous,
            "newStatus", next,
            "updatedBy", updatedBy,
            "updatedAt", String.valueOf(credit.get("updatedAt"))
        );
    }
}
