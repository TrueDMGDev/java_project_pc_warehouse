ALTER TABLE requests
DROP CONSTRAINT chk_requests_status;

ALTER TABLE requests
ADD CONSTRAINT chk_requests_status
CHECK (request_status IN ('PENDING', 'APPROVED', 'REJECTED', 'COMPLETED', 'CANCELLED'));
