# netchdf-kotlin
_last updated: Mar 14, 2023_

This is a rewrite in kotlin of parts of the devcdm and netcdf-java libraries. 

The intention is to create a maintainable, read-only, pure JVM library allowing full access to 
netcdf3, netcdf4, hdf4, hdf5, hdf-eos2 and hdf-eos5 data files. 

Please contact me if you'd like to help out. Especially needed are test datasets from all the important data archives!!

#### Why this library? 

There is so much important scientific data stored in the NetCDF and HDF file formats, that those formats will 
never go away. Its important that there be maintainable, independent libraries to read these files forever.

The Netcdf-Java library prototyped a "Common Data Model" (CDM) to provide a single API to access various file formats. 
The netcdf* and hdf* file formats are similar enough to make a common API a practical and useful goal. 
By focusing on read-only access to just these formats, the API and the code are kept simple.

In short, a library that focuses on simplicity and clarity is a safeguard for the huge investment in these
scientific datasets.

#### Why do we need an alternative library from the standard reference libraries?

The reference libraries are well maintained but complex. They are coded in C, which is a difficult language to master
and keep bug free, with implication for memory safety and security. They require various machine and OS dependent
toolchains. Shifts in funding could wipe out much of the institutional knowledge needed to maintain them.

The HDF file formats are overly complicated, which impacts code complexity and clarity. The data structures do not
always map to a user understandable data model. Semantics are left to the whim of the data-writers to document (or not). 
While this problem isn't specific to HDF file users, its exacerbated by a "group of messages" design approach. Our 
library tries to ameliorate these problems for non-expert readers.

The HDF4 C library is a curious hodgepodge of disjointed APIs. Only some of the APIs needed to fully examine an HDF4 
file in a general way are documented and given examples. This creates barriers to casual use of the C API for reading
HDF4 files.

HDF-EOS use an undocumented "Object Descriptor Language (ODL)" text format, which adds a dependency on the SDP Toolkit 
and possibly other libraries. These toolkits also provide functionality such as handling projections and coordinate system 
conversions, and arguably its impossible to process HDF-EOS without them. So the value added here by an independent 
library for data access only is less clear. For now, we will provide a "best-effort" effort to expose the internal 
contents of the file.

#### Why kotlin?

Kotlin is a modern, statically typed, garbage-collected language suitable for large development projects, 
with many new features for safer and more concise code. It is a clear improvement over Java, without giving
up any of Java's strengths. Kotlin will attract the next generation of serious open-source developers, and 
hopefully some of them will be willing to keep this library working into the unforeseeable future.

### What about performance?

We are aiming to be within 2x of the C library for reading data. Preliminary tests indicate that's mostly within reach. 
For HDF5 files using deflate filters, the deflate library dominates the read time. Standard Java deflate seems to be
about 2X slower than native code.

Its possible we can use kotlin coroutines to speed up performance bottlenecks. TBD.

### Testing

We are using the Foreign Function & Memory API (Java 19 Preview) for testing against the Netcdf C and HDF4 C libraries. 
With these tools we have a good chance of keeping on par with the reference libraries.

### Scope

We do not plan to provide write capabilities. 

The Netcdf-4 library is not tested against HDF5 files that are not written with the Netcdf-4 library. Similarly, 
it provides access to only the HDF4 files using the SDS API. We have the goal to give access to all the content 
in HDF5, HDF4 and HDF-EOS files.

### Data Model notes

(Work in progress)

* Added netcdf4 style typedefs, aka "User defined types": Compound, Enum, Opaque, Vlen.
* Use non-shared dimensions for anonymous dimensions. nclib makes these shared by adding dimensions named "phony_dim_XXX".
* Opaque: hdf5 makes arrays of Opaque all the same size, which gives up some of its usefulness. If theres a need,
  we will allow Opaque(*) indicating that the sizes can vary.