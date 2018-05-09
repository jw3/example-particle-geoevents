--
-- from lagoms read-side offset tracking
-- https://www.lagomframework.com/documentation/1.4.x/scala/ReadSideSlick.html#Building-the-read-side-handler
--


CREATE TABLE read_side_offsets (
  read_side_id VARCHAR(255),
  tag VARCHAR(255),
  sequence_offset bigint,
  time_uuid_offset char(36),
  PRIMARY KEY (read_side_id, tag)
);
