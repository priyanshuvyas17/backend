package com.xray.backend.service;

import com.xray.backend.entity.User;
import com.xray.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class UserService {
  private final UserRepository repo;

  public UserService(UserRepository repo) {
    this.repo = repo;
  }

  @Transactional
  public User create(String name, String email) {
    User u = new User();
    u.setName(name);
    u.setEmail(email);
    return repo.save(u);
  }

  @Transactional(readOnly = true)
  public List<User> list() {
    return repo.findAll();
  }
}
