# Legacy manual network probes

`ProviderUnitTest.kt` is preserved from upstream as a manual example, outside the
normal JVM test source set. It opens real sockets, uses a hard-coded private LAN
address, and imports obsolete plugin SDK classes. It has not been run or counted
as passing validation. Update its client/protocol and configure an explicit test
server before using it; it must not run during the ordinary offline unit suite.
