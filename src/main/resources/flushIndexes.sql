drop schema if exists `__akiban`;
create schema `__akiban`;
create table `__akiban`.`doesntmatter`(doesntmatter varchar(200) default 'flushIndexes') engine=akibadb;
drop schema if exists `__akiban`;

