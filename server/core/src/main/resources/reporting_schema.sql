CREATE DATABASE blynk_reporting;

\connect blynk_reporting

CREATE TABLE reporting_events (
  id bigserial,
  device_id int4,
  type smallint,
  ts timestamp without time zone default (now() at time zone 'utc'),
  event_hashcode int4 DEFAULT 0,
  description text,
  is_resolved boolean DEFAULT FALSE,
  resolved_by text,
  resolved_at timestamp,
  resolved_comment text
);
CREATE INDEX reporting_events_main_idx ON reporting_events (device_id, type, ts);

CREATE TABLE reporting_events_last_seen (
  device_id int4,
  email text,
  ts timestamp without time zone default (now() at time zone 'utc'),
  PRIMARY KEY(device_id, email)
);

CREATE TABLE knight_laundry (
   device_id int4,
   pin int2,
   pin_type int2,
   created timestamp,
   type_of_record int4,
   washer_id int4,
   start_date date,
   start_time time,
   finish_time time,
   cycle_time time,
   formula_number int4,
   load_weight int4,
   pump_id int4,
   volume int4,
   run_time int4,
   pulse_count int4
);
create index on knight_laundry (device_id, pin, pin_type, created);

CREATE TABLE knight_scopetech (
   device_id int4,
   pin int2,
   pin_type int2,
   created timestamp,
   time timestamp,
   scope_user text,
   serial int4,
   dose_volume int4,
   flush_volume int4,
   rinse_volume int4,
   leak_test int4,
   pressure int4,
   temperature int4,
   error int4
);
create index on knight_scopetech (device_id, pin, pin_type, created);

CREATE TABLE blynk_default (
   device_id int4,
   pin int2,
   pin_type int2,
   created timestamp,
   value float8
);
create index on blynk_default (device_id, pin, pin_type, created);

CREATE TABLE reporting_raw_data (
  email text,
  project_id int4,
  device_id int4,
  pin int2,
  pinType char,
  ts timestamp,
  stringValue text,
  doubleValue float8,

  PRIMARY KEY (email, project_id, device_id, pin, pinType, ts)
);

CREATE TABLE reporting_average_minute (
  email text,
  project_id int4,
  device_id int8,
  pin int2,
  pin_type int2,
  ts timestamp with time zone,
  value float8,
  PRIMARY KEY (email, project_id, device_id, pin, pin_type, ts)
);

CREATE TABLE reporting_average_hourly (
  email text,
  project_id int4,
  device_id int8,
  pin int2,
  pin_type int2,
  ts timestamp with time zone,
  value float8,
  PRIMARY KEY (email, project_id, device_id, pin, pin_type, ts)
);

CREATE TABLE reporting_average_daily (
  email text,
  project_id int4,
  device_id int8,
  pin int2,
  pin_type int2,
  ts timestamp with time zone,
  value float8,
  PRIMARY KEY (email, project_id, device_id, pin, pin_type, ts)
);

CREATE TABLE reporting_app_stat_minute (
  region text,
  ts timestamp,
  active int4,
  active_week int4,
  active_month int4,
  minute_rate int4,
  connected int4,
  online_apps int4,
  online_hards int4,
  total_online_apps int4,
  total_online_hards int4,
  registrations int4,
  PRIMARY KEY (region, ts)
);

CREATE TABLE reporting_app_command_stat_minute (
  region text,
  ts timestamp,
  response int4,
  register int4,
  login int4,
  load_profile int4,
  app_sync int4,
  sharing int4,
  get_token int4,
  ping int4,
  activate int4,
  deactivate int4,
  refresh_token int4,
  get_graph_data int4,
  export_graph_data int4,
  set_widget_property int4,
  bridge int4,
  hardware int4,
  get_share_dash int4,
  get_share_token int4,
  refresh_share_token int4,
  share_login int4,
  create_project int4,
  update_project int4,
  delete_project int4,
  hardware_sync int4,
  internal int4,
  sms int4,
  tweet int4,
  email int4,
  push int4,
  add_push_token int4,
  create_widget int4,
  update_widget int4,
  delete_widget int4,
  create_device int4,
  update_device int4,
  delete_device int4,
  get_devices int4,
  create_tag int4,
  update_tag int4,
  delete_tag int4,
  get_tags int4,
  add_energy int4,
  get_energy int4,
  get_server int4,
  connect_redirect int4,
  web_sockets int4,
  eventor int4,
  webhooks int4,
  appTotal int4,
  hardTotal int4,

  PRIMARY KEY (region, ts)
);

CREATE TABLE reporting_http_command_stat_minute (
  region text,
  ts timestamp,
  is_hardware_connected int4,
  is_app_connected int4,
  get_pin_data int4,
  update_pin int4,
  email int4,
  push int4,
  get_project int4,
  qr int4,
  get_history_pin_data int4,
  total int4,
  PRIMARY KEY (region, ts)
);

create user test with password 'test';
GRANT CONNECT ON DATABASE blynk_reporting TO test;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO test;
GRANT ALL ON sequence reporting_events_id_seq to test;