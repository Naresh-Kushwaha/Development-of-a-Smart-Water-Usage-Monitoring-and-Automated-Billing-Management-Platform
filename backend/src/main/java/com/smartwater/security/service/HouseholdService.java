package com.smartwater.service;

import com.smartwater.domain.*;
import com.smartwater.domain.Enums.Role;
import com.smartwater.repo.*;
import com.smartwater.web.ApiException;
import com.smartwater.web.Dtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HouseholdService {
    private final HouseholdRepository households;
    private final ApartmentRepository apartments;
    private final UserRepository users;

    public List<HouseholdRes> list(Long apartmentId) {
        return households.findByApartmentIdOrderByUnitNumber(apartmentId).stream().map(this::toRes).toList();
    }

    @Transactional
    public HouseholdRes create(Long apartmentId, HouseholdReq r) {
        if (households.findByApartmentIdAndUnitNumberIgnoreCase(apartmentId, r.unitNumber().trim()).isPresent())
            throw ApiException.conflict("Unit " + r.unitNumber() + " already exists");
        Household h = new Household();
        h.setApartment(apartments.getReferenceById(apartmentId));
        apply(h, r);
        return toRes(households.save(h));
    }

    @Transactional
    public HouseholdRes update(Long apartmentId, Long id, HouseholdReq r) {
        Household h = find(apartmentId, id);
        households.findByApartmentIdAndUnitNumberIgnoreCase(apartmentId, r.unitNumber().trim())
                .filter(o -> !o.getId().equals(id))
                .ifPresent(o -> { throw ApiException.conflict("Unit " + r.unitNumber() + " already exists"); });
        apply(h, r);
        return toRes(households.save(h));
    }

    @Transactional
    public void delete(Long apartmentId, Long id) { households.delete(find(apartmentId, id)); }

    @Transactional
    public HouseholdRes assign(Long apartmentId, Long id, String email) {
        Household h = find(apartmentId, id);
        User u = users.findByEmail(email.trim().toLowerCase())
                .filter(x -> x.getApartment().getId().equals(apartmentId) && x.getRole() == Role.RESIDENT)
                .orElseThrow(() -> ApiException.bad("No resident with that email in this apartment"));
        u.setHousehold(h);
        users.save(u);
        return toRes(h);
    }

    private Household find(Long apartmentId, Long id) {
        return households.findByIdAndApartmentId(id, apartmentId).orElseThrow(() -> ApiException.notFound("Household"));
    }

    private void apply(Household h, HouseholdReq r) {
        h.setUnitNumber(r.unitNumber().trim());
        h.setAreaSqft(r.areaSqft());
        h.setOccupants(r.occupants());
        h.setMeterNumber(r.meterNumber() == null || r.meterNumber().isBlank() ? null : r.meterNumber().trim());
    }

    private HouseholdRes toRes(Household h) {
        List<String> residents = users.findByHouseholdId(h.getId()).stream().map(User::getEmail).toList();
        return new HouseholdRes(h.getId(), h.getUnitNumber(), h.getAreaSqft(), h.getOccupants(), h.getMeterNumber(),
                h.hasMeter(), residents);
    }
}
