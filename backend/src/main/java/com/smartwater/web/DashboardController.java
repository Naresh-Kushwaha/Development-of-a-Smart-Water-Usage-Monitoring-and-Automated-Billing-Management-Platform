package com.smartwater.web;

import com.smartwater.domain.Notification;
import com.smartwater.repo.NotificationRepository;
import com.smartwater.security.Current;
import com.smartwater.service.DashboardService;
import com.smartwater.web.Dtos.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboard;
    private final NotificationRepository notifications;

    @GetMapping("/dashboard")
    public DashboardRes dashboard() { return dashboard.resident(Current.user()); }

    @GetMapping("/dashboard/compare")
    public CompareRes compare() { return dashboard.compare(Current.user()); }

    @GetMapping("/notifications")
    public List<NotificationRes> notifications() {
        return notifications.findTop50ByUserIdOrderByCreatedAtDesc(Current.user().getId()).stream().map(Mapper::notification).toList();
    }

    @PostMapping("/notifications/{id}/read")
    public void read(@PathVariable Long id) {
        Notification n = notifications.findById(id).filter(x -> x.getUser().getId().equals(Current.user().getId()))
                .orElseThrow(() -> ApiException.notFound("Notification"));
        n.setSeen(true);
        notifications.save(n);
    }

    @PostMapping("/notifications/read-all")
    public void readAll() {
        List<Notification> list = notifications.findTop50ByUserIdOrderByCreatedAtDesc(Current.user().getId());
        list.forEach(n -> n.setSeen(true));
        notifications.saveAll(list);
    }
}
