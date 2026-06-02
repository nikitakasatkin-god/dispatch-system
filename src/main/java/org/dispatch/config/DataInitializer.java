package org.dispatch.config;

import org.dispatch.model.DispatchStatus;
import org.dispatch.model.Role;
import org.dispatch.model.User;
import org.dispatch.repository.DispatchStatusRepository;
import org.dispatch.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DispatchStatusRepository dispatchStatusRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           DispatchStatusRepository dispatchStatusRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.dispatchStatusRepository = dispatchStatusRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Создание пользователей
        if (userRepository.findByUsername("admin").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setFullName("Администратор");
            admin.setRole(Role.ADMIN);
            admin.setActive(true);
            userRepository.save(admin);
        }

        if (userRepository.findByUsername("dispatcher").isEmpty()) {
            User dispatcher = new User();
            dispatcher.setUsername("dispatcher");
            dispatcher.setPassword(passwordEncoder.encode("dispatcher123"));
            dispatcher.setFullName("Диспетчер");
            dispatcher.setRole(Role.DISPATCHER);
            dispatcher.setActive(true);
            userRepository.save(dispatcher);
        }

        // Создание статусов по умолчанию
        if (dispatchStatusRepository.count() == 0) {
            String[][] statuses = {
                    {"NEW", "Новый рейс", "#6b7280", "1"},
                    {"DISPATCHED", "Отправлен", "#3b82f6", "2"},
                    {"IN_TRANSIT", "В пути", "#eab308", "3"},
                    {"ARRIVED", "Прибыл", "#10b981", "4"},
                    {"DELAYED", "Задержка", "#ef4444", "5"},
                    {"COMPLETED", "Завершен", "#059669", "6"},
                    {"CANCELLED", "Отменен", "#6b7280", "7"}
            };

            for (String[] status : statuses) {
                DispatchStatus ds = new DispatchStatus();
                ds.setName(status[0]);
                ds.setDescription(status[1]);
                ds.setColor(status[2]);
                ds.setSortOrder(Integer.parseInt(status[3]));
                ds.setActive(true);
                dispatchStatusRepository.save(ds);
            }
        }

        System.out.println("=== Dispatch System initialized ===");
    }
}