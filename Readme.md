# cdm-kotlin
_last updated: Feb 15, 2023_

This is a rewrite in kotlin (not a port) of parts of the devcdm and netcdf-java libraries. 

The intention is to create a maintainable, alternative, read-only
implementation of the netcdf3, netcdf4, and hdf5 libraries. Maybe add hdf4, hdf-eos2, and hdf-eos5.

Please contact me if you'd like to help out. Especially needed are test datasets from all the important data archives!!

#### Why this library? 

There is so much important scientific data stored in the netcdf and hdf file formats, that those formats will 
never go away. Its important that there be maintainable independent libraries to read these files forever. 

The idea of the Common Data Model (CDM) is to provide a single API to access these various file formats. This idea
formed the basis of the Netcdf-Java library. The Netcdf-4 C library now implements this idea also.

#### Why do we need an alternative library from the standard supported libraries?

The standard libraries are complicated, legacy code in C and C++. Shifts in funding could wipe out some or much of the
institutional knowledge needed to maintain and update them. Possible future security flaws could be a major problem
if there arent experts available to fix them. 

An alternative, read-only library is a good way to safeguard the investment in these scientific datasets.

#### Why kotlin?

Kotlin is a modern language that will attract competent programmers for at least the next 20 years. 
Kotlin runs on the JVM, so theres no need to learn the C/C++ toolchain.
It can make use of the entire Java library ecosystem. Its fun and shiny.

### Testing

We are using the Foreign Function & Memory API (Java 19 Preview) for testing against the netcdf C library. We may also 
add testing against the hdf and hdf-eos libraries. With these tools we have a good chance of keeping the cdm-kotlin
library on par with the reference libraries.

### The cdm-kotlin Data Model

I expect this to diverge from the netcdf-java data model, and more closely align with the netcdf4, hdf5 and hdf-eos data models.

(Work in progress)

* Add netcdf4 typedefs, aka "User defined types": Compound, Enum, Opaque, Vlen.
* Use anonymous dimensions for non-shared dimensions. nclib makes these shared by adding dimensions named "phony_dim_X".
* Opaque: hdf5 makes arrays of Opaque all the same size, which gives up some of its usefulness. If theres a need,
  we will allow Opaque(*) indicating that the sizes can vary.