create table Accounts (
  accountId   VARCHAR(50) NOT NULL,
  password    VARCHAR(50),
  manager     VARCHAR(50),
  middlename  VARCHAR(50),
  firstname   VARCHAR(50) NOT NULL,
  lastname    VARCHAR(50) NOT NULL,
  email       VARCHAR(250),
  department  VARCHAR(250),
  title       VARCHAR(250),
  age         INTEGER,
  accessed    BIGINT,
  salary      DECIMAL(9,2),
  jpegphoto   BYTEA,
  activate    DATE,  
  opentime    TIME,    
  changed     TIMESTAMP NOT NULL,
  changelog   BIGINT
);
