# cdm-kotlin
_last updated: Mar 7, 2023_

This is a rewrite in kotlin of parts of the devcdm and netcdf-java libraries. 

The intention is to create a maintainable, alternative, read-only implementation of the netcdf3, netcdf4, 
hdf4, hdf5, hdf-eos2 and hdf-eos5 libraries. 

Please contact me if you'd like to help out. Especially needed are test datasets from all the important data archives!!

#### Why this library? 

There is so much important scientific data stored in the NetCDF and HDF file formats, that those formats will 
never go away. Its important that there be maintainable, independent libraries to read these files forever. 

The goal of the Common Data Model (CDM) is to provide a single API to access these various file formats. This idea
formed the basis of the Netcdf-Java library. The Netcdf-4 C library now implements a version of this.

#### Why do we need an alternative library from the standard reference libraries?

The reference libraries are complex. They are coded in C, requiring cmake and compiler toolchains, compiled for
specific environment. Shifts in funding could wipe out much of the institutional knowledge needed to maintain them.

A read-only library that focuses on simplicity and clarity is a good safeguard for the huge investment in these 
scientific datasets.

#### Why kotlin?

Kotlin is a modern, statically typed language suitable for large development projects, 
with many new features for safer and more concise code. It is a clear improvement over Java, without giving
up any of Java's strengths. Kotlin will attract the next generation of serious programmers. 
Its fun and shiny.

### What about performance?

We are aiming to be within 2x of the C library for reading data. Preliminary tests indicate that's mostly within reach. 
For HDF5 files using deflate filters, the deflate library dominates the read time. Standard java deflate seems to be
about 2X slower than native code.

### Testing

We are using the Foreign Function & Memory API (Java 19 Preview) for testing against the Netcdf C library. 
With these tools we have a good chance of keeping the cdm-kotlin library on par with the reference libraries.

### Scope

The Netcdf-4 library does not work with all HDF5 files not written with the Netcdf4 library API.
The CDM model has the goal to give access to all HDF5 files.

Similarly, the Netcdf-4 library only works with HDF4 files written with the SDS api, while the CDM model has the
goal to give access to all HDF4 and HDF-EOS files.

### The cdm-kotlin Data Model

(Work in progress)

* Add netcdf4 typedefs, aka "User defined types": Compound, Enum, Opaque, Vlen.
* Use non-shared dimensions for anonymous dimensions. nclib makes these shared by adding dimensions named "phony_dim_XXX".
* Opaque: hdf5 makes arrays of Opaque all the same size, which gives up some of its usefulness. If theres a need,
  we will allow Opaque(*) indicating that the sizes can vary.