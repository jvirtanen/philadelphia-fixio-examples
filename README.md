Philadelphia Fixio Examples
===========================

Philadelphia Fixio Examples implements an example [Philadelphia][] client that
is comparable to the example [Fixio][] client.

  [Philadelphia]: https://github.com/paritytrading/philadelphia
  [Fixio]: https://github.com/kpavlov/fixio/


Performance
-----------

Running a Fixio client and a Philadelphia client against a Fixio server yield
the following results:

| Client       | Throughput        | Factor
|--------------|-------------------|-------
| Fixio        | 11994.53 quotes/s |
| Philadelphia | 59413.77 quotes/s |   5.0x

These results are obtained on an m4.xlarge AWS EC2 instance running 64-bit Amazon
Linux and 64-bit OpenJDK 1.8.

The number of quotes is reduced from 1,000,000 to 100,000 to reduce memory
pressure on the Fixio server. Additionally, both the Fixio client and the Fixio
server are given 6 GiB of heap: `-Xms6G -Xmx6G`.

The throughput is an average over 10 sequential runs.

Note that the Philadelphia client would achieve much higher throughput against
a Philadelphia server.


Usage
-----

Run the Philadelphia client with Java:

    java -jar <executable>


License
-------

Philadelphia Fixio Examples is released under the Apache License, Version 2.0.
See `LICENSE` for details.
