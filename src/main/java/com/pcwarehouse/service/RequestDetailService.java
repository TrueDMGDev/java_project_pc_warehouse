package com.pcwarehouse.service;

import com.pcwarehouse.db.DatabaseConnection;
import com.pcwarehouse.model.RequestDetailRecord;
import com.pcwarehouse.model.UserSession;
import com.pcwarehouse.repository.RequestRepository;

import java.sql.Connection;
import java.sql.SQLException;

public final class RequestDetailService {

    private final RequestRepository requestRepository = new RequestRepository();

    public RequestDetailRecord load(UserSession session, String requestNumber) {
        if (requestNumber == null || requestNumber.isBlank()) {
            return null;
        }

        try (Connection connection = DatabaseConnection.open()) {
            return requestRepository.findRequestDetail(connection, session, requestNumber);
        } catch (SQLException exception) {
            return null;
        }
    }
}
