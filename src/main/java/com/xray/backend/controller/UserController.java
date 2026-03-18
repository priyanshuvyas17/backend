package com.xray.backend.controller;

import com.xray.backend.entity.User;
import com.xray.backend.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

record CreateUserRequest(String name, String email) {}

@RestController
@RequestMapping("/api/users")
public class UserController {
  private final UserService userService;

  public UserController(UserService userService) {
    this.userService = userService;
  }

  @PostMapping
  public ResponseEntity<User> create(@RequestBody CreateUserRequest req) {
    User u = userService.create(req.name(), req.email());
    return ResponseEntity.ok(u);
  }

  @GetMapping
  public ResponseEntity<List<User>> list() {
    return ResponseEntity.ok(userService.list());
  }
}
