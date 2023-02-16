# cdm-kotlin
_last updated: Feb 15, 2023_

This is a rewrite in kotlin (not a port) of parts of the devcdm and netcdf-java libraries. 

The intention is to create a maintainable, alternative, read-only
implementation of the netcdf3, netcdf4, and hdf5 libraries. Maybe add hdf4, hdf-eos2, and hdf-eos5.

Please contact me if you'd like to help out. Especially needed are test datasets!

#### Why this library? 

There is so much important scientific data stored in the netcdf and hdf file formats, that those formats will 
never go away. Its important that there be maintainable independent libraries to read these files forever. 

#### Why do we need an alternative library from the standard supported libraries?

The standard libraries are complicated, legacy code in C and C++. Shifts in funding could wipe out some or most of the
institutional knowledge needed to maintain and update them. Possible future security flaws could be a major problem.

An alternative, read-only library is a good way to safeguard the investment in these scientific datasets.

#### Why kotlin?

Kotlin is a modern language that could attract competent programmers for the next 20-50 years. It can make use of the
entire Java library ecosystem. Its fun and shiny.

### Testing

We are using the Foreign Function & Memory API (Java 19 Preview) for testing against the netcdf C library. We may also 
add testing against the hdf and hdf-eos libraries.

### Cdmk Data Model

I expect this to diverge from netcdf-java data model, and more closely align with the netcdf4, hdf5 and hdf-eos data models.

Work in progress

* Add netcdf4 types: Compound, Enum, Opaque, Vlen.
* Anonymous dimensions: for non-shared dimensions. nc4 makes into shared by adding phony_dim_X, dont plan to do that.
* Opaque: hdf5 makes arrays of Opaque all the same size, which gives up some of the usefulness. If theres a need,
  we will allow Opaque(*) indicating that the sizes can vary.