# netchdf-kotlin
_last updated: 4/8/2023_

This is a rewrite in kotlin of parts of the devcdm and netcdf-java libraries. 

The intention is to create a maintainable, read-only, pure JVM library allowing full access to 
netcdf3, netcdf4, hdf4, hdf5, hdf-eos2 and hdf-eos5 data files. 

Please contact me if you'd like to help out. Especially needed are test datasets from all the important data archives!!

#### Why this library? 

There is so much important scientific data stored in the NetCDF and HDF file formats, that those formats will 
never go away. It is important that there be maintainable, independent libraries to read these files forever.

The Netcdf-Java library prototyped a "Common Data Model" (CDM) to provide a single API to access various file formats. 
The netcdf* and hdf* file formats are similar enough to make a common API a practical and useful goal. 
By focusing on read-only access to just these formats, the API and the code are kept simple.

In short, a library that focuses on simplicity and clarity is a safeguard for the huge investment in these
scientific datasets.

#### Why do we need an alternative library from the standard reference libraries?

The reference libraries are well maintained but complex. They are coded in C, which is a difficult language to master
and keep bug free, with implication for memory safety and security. The libraries require various machine and OS dependent
toolchains. Shifts in funding could wipe out much of the institutional knowledge needed to maintain them.

The HDF file formats are overly complicated, which impacts code complexity and clarity. The data structures do not
always map to a user understandable data model. Semantics are left to data-writers to document (or not). 
While this problem isn't specific to HDF file users, it is exacerbated by a "group of messages" design approach. 

The HDF4 C library is a curious hodgepodge of disjointed APIs. The HDF5 API is better and the Netcdf4 API much better.
But all suffer from the limitations of the C language, the difficulty of writing good documentation for all skill levels 
of programmers, and the need to support legacy APIs. 

HDF-EOS uses an undocumented "Object Descriptor Language (ODL)" text format, which adds a dependency on the SDP Toolkit 
and possibly other libraries. These toolkits also provide functionality such as handling projections and coordinate system 
conversions, and arguably its impossible to process HDF-EOS without them. So the value added here by an independent 
library for data access is less clear. For now, we will provide a "best-effort" to expose the internal 
contents of the file.

Currently, the Netcdf-4 and HDF4 libraries are not thread safe, even when operating on different files.
The HDF5 library can be built with MPI-IO for parallel file systems. The serial HDF5 library is apparently thread safe 
but does not support concurrent reading. These are serious limitations for high performance, scalable applications.

Our library tries to ameliorate these problems for scientists and the interested public to access the data without
having to become specialists in the file formats.

#### Why kotlin?

Kotlin is a modern, statically typed, garbage-collected language suitable for large development projects. 
It has many new features for safer (like null-safety) and more concise (like functional idioms) code, and is an important 
improvement over Java, without giving up any of Java's strengths. Kotlin will attract the next generation of serious 
open-source developers, and hopefully some of them will be willing to keep this library working into the unforeseeable future.

### What about performance?

We are aiming to be within 2x of the C libraries for reading data. Preliminary tests indicate that's a reasonable goal. 
For HDF5 files using deflate filters, the deflate library dominates the read time, and standard Java deflate libraries 
are about 2X slower than native code.Unless the deflate libraries get better, theres not much gain in trying to make
other parts of the code faster.

Its possible we can use kotlin coroutines to speed up performance bottlenecks. TBD.

### Testing

We are using the Foreign Function & Memory API (Java 19 Preview) for testing against the Netcdf C, HDF5, and HDF4 C libraries. 
With these tools we can be confident that our library gives the same results as the reference libraries.

Currently (3/27/23) we have test coverage of 77.4% (5136/6632) LOC for the code classes.

We have ~1680 test files. 

````
hdf-eos2 = 274 files
hdf4     = 203 files
hdf5     = 215 files
hdf-eos5 = 28 files
netcdf3  = 812 files
netcdf4  = 147 files
````

We need to get representative samples of recent files for improved testing and code coverage.

### Scope

We have the goal to give read access to all the content in Netcdf, HDF5, HDF4 and HDF-EOS files. 

The library will be thread-safe for reading multiple files concurrently.

We are focussing on earth science data, and dont plan to support other uses except as a byproduct.

We do not plan to provide write capabilities.

### Data Model notes

(Work in progress)

#### Differ from Netcdf4 and CDM data models
* Added netcdf4 style typedefs, aka "User defined types": Compound, Enum, Opaque, Vlen.
* Use non-shared dimensions for anonymous dimensions. nclib makes these shared by adding dimensions named "phony_dim_XXX".
* Datatype.REFERENCE is added
* Opaque, Vlen typedefs ??

#### Differ from HDF5 data model
* Creation order is ignored
* Not including symbolic links in a group, as these point to an existing dataset (variable)
* Opaque: hdf5 makes arrays of Opaque all the same size, which gives up some of its usefulness. If theres a need,
  we will allow Opaque(*) indicating that the sizes can vary.
* Attributes can be of type REFERENCE, with value the full path name of the referenced dataset.