package com.smartwater.web;

import com.smartwater.security.Current;
import com.smartwater.service.HouseholdService;
import com.smartwater.web.Dtos.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/households")
@RequiredArgsConstructor
public class HouseholdController {
    private final HouseholdService service;

    @GetMapping
    public List<HouseholdRes> list() { return service.list(Current.apartmentId()); }

    @PostMapping
    public HouseholdRes create(@Valid @RequestBody HouseholdReq r) { return service.create(Current.apartmentId(), r); }

    @PutMapping("/{id}")
    public HouseholdRes update(@PathVariable Long id, @Valid @RequestBody HouseholdReq r) {
        return service.update(Current.apartmentId(), id, r);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) { service.delete(Current.apartmentId(), id); }

    @PostMapping("/{id}/assign")
    public HouseholdRes assign(@PathVariable Long id, @Valid @RequestBody AssignReq r) {
        return service.assign(Current.apartmentId(), id, r.email());
    }
}
