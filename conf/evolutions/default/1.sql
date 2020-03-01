# --- !Ups

CREATE TABLE Stocks (
    id bigint(20) NOT NULL AUTO_INCREMENT,
    symbol varchar(255) NOT NULL,
    name varchar(255) NOT NULL,
    current_price decimal NOT NULL,
    PRIMARY KEY (symbol)
);

# --- !Downs

drop table "stocks" if exists;
