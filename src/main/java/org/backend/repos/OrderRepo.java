package org.backend.repos;

import org.backend.models.Order;
import org.backend.models.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepo extends JpaRepository<Order, Integer> {
    List<Order> findByUser(User user);
}
