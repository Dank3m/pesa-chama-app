-- V3__In_App_Notifications.sql
-- Create in-app notifications table for real-time notifications

CREATE TABLE in_app_notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    group_id UUID REFERENCES groups(id) ON DELETE SET NULL,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    reference_type VARCHAR(50),
    reference_id UUID,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP WITH TIME ZONE,
    actor_name VARCHAR(100),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE
);

-- Indexes for efficient querying
CREATE INDEX idx_notification_user_read ON in_app_notifications(user_id, is_read);
CREATE INDEX idx_notification_user_created ON in_app_notifications(user_id, created_at DESC);
CREATE INDEX idx_notification_group ON in_app_notifications(group_id);

-- Comment
COMMENT ON TABLE in_app_notifications IS 'Stores in-app notifications for real-time WebSocket delivery';
