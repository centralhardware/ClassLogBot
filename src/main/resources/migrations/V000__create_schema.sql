--
-- Initial schema creation - compatible with existing databases
--

--
-- Name: log_table_changes(); Type: FUNCTION; Schema: public; Owner: postgres
--

CREATE OR REPLACE FUNCTION public.log_table_changes() RETURNS trigger
    LANGUAGE plpgsql
    AS $$
DECLARE
pk_value TEXT;
    diff JSONB;
BEGIN
    -- Получение значения первичного ключа
    IF TG_OP = 'UPDATE' OR TG_OP = 'DELETE' THEN
        pk_value := OLD.id::TEXT;
ELSE
        pk_value := NEW.id::TEXT;
END IF;

    -- Вычисление дифа только для операций UPDATE
    IF TG_OP = 'UPDATE' THEN
        diff := (
            SELECT jsonb_object_agg(key, new_value)
            FROM (
                     SELECT
                         new_data.key,
                         new_data.value AS new_value,
                         old_data.value AS old_value
                     FROM jsonb_each(row_to_json(NEW)::jsonb) AS new_data(key, value)
                              FULL OUTER JOIN jsonb_each(row_to_json(OLD)::jsonb) AS old_data(key, value)
                                              ON new_data.key = old_data.key
                     WHERE new_data.value IS DISTINCT FROM old_data.value
                 ) AS changes
        );
ELSE
        diff := NULL;
END IF;

INSERT INTO log_table (table_name, operation, pk_value, old_data, new_data, diff, changed_at)
VALUES (
           TG_TABLE_NAME,
           TG_OP,
           pk_value,
           row_to_json(OLD),
           row_to_json(NEW),
           diff,
           CURRENT_TIMESTAMP
       );
RETURN NEW;
END;
$$;


-- ALTER FUNCTION public.log_table_changes() OWNER TO postgres;

--
-- Name: client_id_sequence; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE IF NOT EXISTS public.client_id_sequence
    START WITH 269
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


-- ALTER SEQUENCE public.client_id_sequence OWNER TO postgres;

--
-- Name: client; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.client (
                               id integer DEFAULT nextval('public.client_id_sequence'::regclass),
                               create_date timestamp without time zone,
                               last_name character varying(255),
                               modify_date timestamp without time zone,
                               name character varying(255),
                               second_name character varying(255),
                               created_by bigint,
                               update_by bigint,
                               deleted boolean,
                               properties jsonb,
                               klass integer,
                               record_date date,
                               birth_date date,
                               source text,
                               phone text,
                               responsible_phone text,
                               mother_fio text
);


-- ALTER TABLE public.client OWNER TO postgres;

--
-- Name: config; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.config (
                               key text,
                               value text
);


-- ALTER TABLE public.config OWNER TO postgres;

--
-- Name: log_table; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.log_table (
                                  log_id integer NOT NULL,
                                  table_name text,
                                  operation text,
                                  old_data jsonb,
                                  new_data jsonb,
                                  changed_at timestamp without time zone DEFAULT CURRENT_TIMESTAMP,
                                  pk_value text,
                                  diff jsonb
);


-- ALTER TABLE public.log_table OWNER TO postgres;

--
-- Name: log_table_log_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE IF NOT EXISTS public.log_table_log_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


-- ALTER SEQUENCE public.log_table_log_id_seq OWNER TO postgres;

--
-- Name: log_table_log_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

-- ALTER SEQUENCE public.log_table_log_id_seq OWNED BY public.log_table.log_id;


--
-- Name: payment_id_sequence; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE IF NOT EXISTS public.payment_id_sequence
    START WITH 320
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


-- ALTER SEQUENCE public.payment_id_sequence OWNER TO postgres;

--
-- Name: payment; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.payment (
                                id integer DEFAULT nextval('public.payment_id_sequence'::regclass),
                                chat_id bigint NOT NULL,
                                pupil_id integer,
                                amount integer,
                                date_time timestamp without time zone,
                                is_deleted boolean DEFAULT false,
                                properties jsonb DEFAULT '[]'::jsonb,
                                services integer NOT NULL,
                                photo_report text
);


-- ALTER TABLE public.payment OWNER TO postgres;

--
-- Name: service_id_sequence; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE IF NOT EXISTS public.service_id_sequence
    START WITH 712
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


-- ALTER SEQUENCE public.service_id_sequence OWNER TO postgres;

--
-- Name: service; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.service (
                                unique_id integer DEFAULT nextval('public.service_id_sequence'::regclass),
                                id uuid,
                                chat_id bigint,
                                amount integer,
                                pupil_id integer,
                                service_id integer,
                                date_time timestamp without time zone,
                                is_deleted boolean DEFAULT false,
                                properties jsonb,
                                force_group boolean DEFAULT false NOT NULL,
                                update_time timestamp without time zone DEFAULT now(),
                                extra_half_hour boolean DEFAULT false,
                                photo_report text
);


-- ALTER TABLE public.service OWNER TO postgres;

--
-- Name: services; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.services (
                                 id integer NOT NULL,
                                 name text,
                                 allow_multiply_clients boolean DEFAULT false
);


-- ALTER TABLE public.services OWNER TO postgres;

--
-- Name: services_id_seq; Type: SEQUENCE; Schema: public; Owner: postgres
--

CREATE SEQUENCE IF NOT EXISTS public.services_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


-- ALTER SEQUENCE public.services_id_seq OWNER TO postgres;

--
-- Name: services_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: postgres
--

-- ALTER SEQUENCE public.services_id_seq OWNED BY public.services.id;


--
-- Name: telegram_users; Type: TABLE; Schema: public; Owner: postgres
--

CREATE TABLE IF NOT EXISTS public.telegram_users (
                                       id bigint NOT NULL,
                                       services text,
                                       name text,
                                       permissions text[]
);


-- ALTER TABLE public.telegram_users OWNER TO postgres;

--
-- Name: log_table log_id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.log_table ALTER COLUMN log_id SET DEFAULT nextval('public.log_table_log_id_seq'::regclass);


--
-- Name: services id; Type: DEFAULT; Schema: public; Owner: postgres
--

ALTER TABLE ONLY public.services ALTER COLUMN id SET DEFAULT nextval('public.services_id_seq'::regclass);


--
-- Name: log_table log_table_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'log_table_pkey') THEN
        ALTER TABLE public.log_table ADD CONSTRAINT log_table_pkey PRIMARY KEY (log_id);
    END IF;
END $$;


--
-- Name: services services_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'services_pkey') THEN
        ALTER TABLE public.services ADD CONSTRAINT services_pkey PRIMARY KEY (id);
    END IF;
END $$;


--
-- Name: telegram_users telegram_users_pkey; Type: CONSTRAINT; Schema: public; Owner: postgres
--

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'telegram_users_pkey') THEN
        ALTER TABLE public.telegram_users ADD CONSTRAINT telegram_users_pkey PRIMARY KEY (id);
    END IF;
END $$;


--
-- Name: client log_changes_trigger; Type: TRIGGER; Schema: public; Owner: postgres
--

DROP TRIGGER IF EXISTS log_changes_trigger ON public.client;
CREATE TRIGGER log_changes_trigger AFTER INSERT OR DELETE OR UPDATE ON public.client FOR EACH ROW EXECUTE FUNCTION public.log_table_changes();


--
-- Name: config log_changes_trigger; Type: TRIGGER; Schema: public; Owner: postgres
--

DROP TRIGGER IF EXISTS log_changes_trigger ON public.config;
CREATE TRIGGER log_changes_trigger AFTER INSERT OR DELETE OR UPDATE ON public.config FOR EACH ROW EXECUTE FUNCTION public.log_table_changes();


--
-- Name: payment log_changes_trigger; Type: TRIGGER; Schema: public; Owner: postgres
--

DROP TRIGGER IF EXISTS log_changes_trigger ON public.payment;
CREATE TRIGGER log_changes_trigger AFTER INSERT OR DELETE OR UPDATE ON public.payment FOR EACH ROW EXECUTE FUNCTION public.log_table_changes();


--
-- Name: service log_changes_trigger; Type: TRIGGER; Schema: public; Owner: postgres
--

DROP TRIGGER IF EXISTS log_changes_trigger ON public.service;
CREATE TRIGGER log_changes_trigger AFTER INSERT OR DELETE OR UPDATE ON public.service FOR EACH ROW EXECUTE FUNCTION public.log_table_changes();


--
-- Name: services log_changes_trigger; Type: TRIGGER; Schema: public; Owner: postgres
--

DROP TRIGGER IF EXISTS log_changes_trigger ON public.services;
CREATE TRIGGER log_changes_trigger AFTER INSERT OR DELETE OR UPDATE ON public.services FOR EACH ROW EXECUTE FUNCTION public.log_table_changes();


--
-- Name: telegram_users log_changes_trigger; Type: TRIGGER; Schema: public; Owner: postgres
--

DROP TRIGGER IF EXISTS log_changes_trigger ON public.telegram_users;
CREATE TRIGGER log_changes_trigger AFTER INSERT OR DELETE OR UPDATE ON public.telegram_users FOR EACH ROW EXECUTE FUNCTION public.log_table_changes();


--
-- Name: payment services_fk; Type: FK CONSTRAINT; Schema: public; Owner: postgres
--

DO $$ BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'services_fk') THEN
        ALTER TABLE public.payment ADD CONSTRAINT services_fk FOREIGN KEY (services) REFERENCES public.services(id);
    END IF;
END $$;


--
-- Initial schema migration complete
--

