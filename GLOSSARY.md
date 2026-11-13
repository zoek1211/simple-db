# SimpleDb

A small utility that wraps JDBC so MySQL queries can be written and run with little ceremony.

## Language

**SimpleDb**:
The entry point that holds the connection details for one database and hands out Sql objects.
_Avoid_: DB manager, data source

**Session**:
One thread's own connection to the database; it starts on first use and ends when that thread calls close.
_Avoid_: Shared connection, pool

**Close**:
Ending the calling thread's session only; other threads' sessions are untouched, and the next use opens a fresh session.
_Avoid_: Shutdown, dispose

**Transaction**:
One thread's unit of work inside its session, from startTransaction to commit or rollback; it never spans threads and does not nest.
_Avoid_: Global transaction

**Sql**:
A single query being built up piece by piece and then run once.
_Avoid_: Query builder, statement

**Dev mode**:
A setting that makes every query print its SQL text and bound values before it runs.
_Avoid_: Debug mode, verbose mode
