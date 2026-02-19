DO
$$BEGIN
    IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'spiskammerset-opprydding-dev') THEN
        GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO "spiskammerset-opprydding-dev";
        GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO "spiskammerset-opprydding-dev";
END IF;
END$$;
