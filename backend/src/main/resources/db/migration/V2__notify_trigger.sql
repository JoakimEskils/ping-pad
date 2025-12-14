-- PostgreSQL NOTIFY trigger for event subscription processing
-- This trigger sends notifications when new events are inserted
-- Note: This migration requires V1__eventsourcing_tables.sql to run first

-- Create the function (safe to run multiple times)
CREATE OR REPLACE FUNCTION notify_new_event()
RETURNS TRIGGER AS $$
BEGIN
    PERFORM pg_notify('new_event', NEW.aggregate_id::text || ',' || NEW.id::text);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Only create trigger if table exists
DO $$
BEGIN
    IF EXISTS (
        SELECT FROM information_schema.tables 
        WHERE table_schema = 'public' 
        AND table_name = 'es_event'
    ) THEN
        -- Drop trigger if it exists
        DROP TRIGGER IF EXISTS trigger_notify_new_event ON es_event;
        
        -- Create the trigger
        CREATE TRIGGER trigger_notify_new_event
            AFTER INSERT ON es_event
            FOR EACH ROW
            EXECUTE FUNCTION notify_new_event();
    END IF;
END $$;
