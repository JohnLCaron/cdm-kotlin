package com.sunya.netchdf.netcdf3

import java.util.*
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

class N3headerTest {

    companion object {
        @JvmStatic
        fun params(): Stream<Arguments> = Stream.of(
            Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf3/simple_xy.nc",
"""  dimensions:
    x = 6;
    y = 12;
  variables:
    int data(x=6, y=12);

"""),
            Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf3/longOffset.nc",
                """  dimensions:
    len_string = 33;
    len_line = 81;
    four = 4;
    time_step = UNLIMITED;   // (0 currently)
    num_qa_rec = 1;
    num_dim = 2;
    num_nodes = 121;
    num_elem = 100;
    num_el_blk = 1;
    num_el_in_blk1 = 100;
    num_nod_per_el1 = 4;
  variables:
    float time_whole(time_step=0);

    char qa_records(num_qa_rec=1, four=4, len_string=33);

    int eb_status(num_el_blk=1);

    int eb_prop1(num_el_blk=1);
      :name = "ID";

    float coordx(num_nodes=121);

    float coordy(num_nodes=121);

    char coor_names(num_dim=2, len_string=33);

    int elem_map(num_elem=100);

    int connect1(num_el_in_blk1=100, num_nod_per_el1=4);
      :elem_type = "QUAD";

  // global attributes:
  :api_version = 4.01f;
  :version = 3.01f;
  :floating_point_word_size = 4;
  :file_size = 1;
  :title = "box";
"""),
            Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf3/WMI_Lear-2003-05-28-212817.nc",
"""  dimensions:
    time = UNLIMITED;   // (588 currently)
  variables:
    double time(time=588);
      :units = "seconds since 1970-1-1 0:00:00 0:00";
      :long_name = "time";
      :_FillValue = -99999.0;
      :missing_value = -99999.0;

    float altitude(time=588);
      :units = "km";
      :long_name = "altitude MSL";
      :_FillValue = -99999.0f;
      :missing_value = -99999.0f;

    float latitude(time=588);
      :units = "degrees_N";
      :long_name = "latitude";
      :_FillValue = -99999.0f;
      :missing_value = -99999.0f;
      :valid_range = -90.0f, 90.0f;

    float longitude(time=588);
      :units = "degrees_E";
      :long_name = "longitude";
      :_FillValue = -99999.0f;
      :missing_value = -99999.0f;
      :valid_range = -180.0f, 180.0f;

    float pressure(time=588);
      :units = "hPa";
      :long_name = "pressure";
      :field_type = "P";
      :_FillValue = -99999.0f;
      :missing_value = -99999.0f;

    float tdry(time=588);
      :units = "deg_C";
      :long_name = "temperature";
      :field_type = "T";
      :_FillValue = -99999.0f;
      :missing_value = -99999.0f;

    float dp(time=588);
      :units = "deg_C";
      :long_name = "dewpoint temperature";
      :field_type = "T_d";
      :_FillValue = -99999.0f;
      :missing_value = -99999.0f;

    float mr(time=588);
      :units = "g/kg";
      :long_name = "mixing ratio";
      :field_type = "w";
      :_FillValue = -99999.0f;
      :missing_value = -99999.0f;

    float wspd(time=588);
      :units = "m/s";
      :long_name = "wind speed";
      :field_type = "wspd";
      :_FillValue = -99999.0f;
      :missing_value = -99999.0f;

    float wdir(time=588);
      :units = "degrees";
      :long_name = "wind direction";
      :field_type = "wdir";
      :_FillValue = -99999.0f;
      :missing_value = -99999.0f;

    int Drops(time=588);
      :units = "#";
      :long_name = "Accumulated number of sonde drops";
      :_FillValue = -99999;
      :missing_value = -99999;

  // global attributes:
  :history = "${'$'}Id: TrackFile.java,v 1.20 2003/05/07 04:53:23 maclean Exp ${'$'}";
"""),
            Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf3/nctest_64bit_offset.nc",
                """  dimensions:
    ii = 4;
    jj = 3;
    kk = 3;
    i1 = 5;
    i2 = 3;
    i3 = 7;
    rec = UNLIMITED;   // (3 currently)
    ll = 3;
    mm = 1;
    nn = 1;
    pp = 7;
    qq = 10;
    d0 = 2;
    d1 = 3;
    d2 = 5;
    d3 = 6;
    d4 = 4;
    d5 = 31;
    w = 7;
    x = 5;
    y = 6;
    z = 4;
  variables:
    int aa(ii=4);
      :units = "furlongs";

    int bb(kk=3, jj=3);
      :valid_range = 0.0f, 100.0f;

    int cc(rec=3);
      :units = "moles";

    short cd(rec=3, i2=3);
      :units = "moles";

    float ce(rec=3, i2=3, i3=7);
      :units = "moles";

    short dd(ll=3);
      :fill_value = -999S;

    byte bytev(d0=2, d1=3, d2=5, d3=6, d4=4, d5=31);

    char charv(d0=2, d1=3, d2=5, d3=6, d4=4);

    short shortv(d0=2, d1=3, d2=5, d3=6);

    int longv(d0=2, d1=3, d2=5);

    float floatv(d0=2, d1=3);

    double doublev(d0=2);

    double scalarv;

    float xx(ii=4);

    byte bytevar(w=7, x=5, y=6, z=4);

    char charvar(w=7, x=5, y=6, z=4);

    short shortvar(w=7, x=5, y=6, z=4);

    int longvar(w=7, x=5, y=6, z=4);

    float floatvar(w=7, x=5, y=6, z=4);

    double doublevar(w=7, x=5, y=6, z=4);

    short yy(ii=4);

    byte zz(ii=4, jj=3);

    int ww(ii=4);
      :att0 = "chars";
      :att1 = 97B, 98B;
      :att2 = "chars";
      :att3 = -999S, 0S, 999S;
      :att4 = 10, 20;
      :att5 = 1.5f, 2.5f, 3.5f;

    short vv(ii=4, jj=3);

    int uu(ii=4, jj=3);
      :valid_max = 1000;

    int tt(ii=4);
      :att = -1.0, -2.0;

    double tu(ii=4, jj=3);
      :att = -1.0f, -2.0f;

    double yet_another_variable(ii=4, jj=3);
      :yet_another_attribute = -1S, -2S, -3S;

  // global attributes:
  :title = "test netcdf";
  :att0 = 97B, 98B;
  :att1 = "chars";
  :att2 = -999S, 0S, 999S;
  :att3 = 10, 20;
  :att4 = 1.5f, 2.5f, 3.5f;
  :att5 = 4.5, 5.5, 6.5, 7.5;
  :att = -1.0f, -2.0f;
  :attx = 3S, 4S, 5S;
  :plugh = 3S, 4S, 5S;
  :longer_name = 3S, 4S, 5S;
  :yet_another_attribute = -1S, -2S, -3S;
"""),
            Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf3/WrfTimesStrUnderscore.nc",
                """  dimensions:
    Time = UNLIMITED;   // (3 currently)
    south_north = 1;
    west_east = 1;
    DateStrLen = 19;
  variables:
    char Times(Time=3, DateStrLen=19);

  // global attributes:
  :TITLE = " OUTPUT FROM WRF V3.5 MODEL";
  :START_DATE = "2008-06-20_00:00:00";
  :SIMULATION_START_DATE = "2008-06-20_00:00:00";
  :WEST-EAST_GRID_DIMENSION = 330;
  :SOUTH-NORTH_GRID_DIMENSION = 270;
  :BOTTOM-TOP_GRID_DIMENSION = 30;
  :DX = 30000.0f;
  :DY = 30000.0f;
  :STOCH_FORCE_OPT = 0;
  :GRIDTYPE = "C";
  :DIFF_OPT = 1;
  :KM_OPT = 4;
  :DAMP_OPT = 0;
  :DAMPCOEF = 0.2f;
  :KHDIF = 0.0f;
  :KVDIF = 0.0f;
  :MP_PHYSICS = 3;
  :RA_LW_PHYSICS = 1;
  :RA_SW_PHYSICS = 1;
  :SF_SFCLAY_PHYSICS = 1;
  :SF_SURFACE_PHYSICS = 2;
  :BL_PBL_PHYSICS = 1;
  :CU_PHYSICS = 1;
  :SURFACE_INPUT_SOURCE = 1;
  :SST_UPDATE = 0;
  :GRID_FDDA = 0;
  :GFDDA_INTERVAL_M = 0;
  :GFDDA_END_H = 0;
  :GRID_SFDDA = 0;
  :SGFDDA_INTERVAL_M = 0;
  :SGFDDA_END_H = 0;
  :HYPSOMETRIC_OPT = 2;
  :SF_URBAN_PHYSICS = 0;
  :SHCU_PHYSICS = 0;
  :MFSHCONV = 0;
  :FEEDBACK = 1;
  :SMOOTH_OPTION = 0;
  :SWRAD_SCAT = 1.0f;
  :W_DAMPING = 1;
  :MOIST_ADV_OPT = 1;
  :SCALAR_ADV_OPT = 1;
  :TKE_ADV_OPT = 1;
  :DIFF_6TH_OPT = 0;
  :DIFF_6TH_FACTOR = 0.12f;
  :TOPO_WIND = 1;
  :OBS_NUDGE_OPT = 0;
  :BUCKET_MM = -1.0f;
  :BUCKET_J = -1.0f;
  :PREC_ACC_DT = 0.0f;
  :SF_OCEAN_PHYSICS = 0;
  :ISFTCFLX = 0;
  :ISHALLOW = 0;
  :DFI_OPT = 0;
  :WEST-EAST_PATCH_START_UNSTAG = 1;
  :WEST-EAST_PATCH_END_UNSTAG = 329;
  :WEST-EAST_PATCH_START_STAG = 1;
  :WEST-EAST_PATCH_END_STAG = 330;
  :SOUTH-NORTH_PATCH_START_UNSTAG = 1;
  :SOUTH-NORTH_PATCH_END_UNSTAG = 269;
  :SOUTH-NORTH_PATCH_START_STAG = 1;
  :SOUTH-NORTH_PATCH_END_STAG = 270;
  :BOTTOM-TOP_PATCH_START_UNSTAG = 1;
  :BOTTOM-TOP_PATCH_END_UNSTAG = 29;
  :BOTTOM-TOP_PATCH_START_STAG = 1;
  :BOTTOM-TOP_PATCH_END_STAG = 30;
  :GRID_ID = 1;
  :PARENT_ID = 1;
  :I_PARENT_START = 1;
  :J_PARENT_START = 1;
  :PARENT_GRID_RATIO = 1;
  :DT = 180.0f;
  :CEN_LAT = -25.00002f;
  :CEN_LON = 130.0f;
  :TRUELAT1 = -25.0f;
  :TRUELAT2 = -25.0f;
  :MOAD_CEN_LAT = -25.00002f;
  :STAND_LON = 130.0f;
  :POLE_LAT = 90.0f;
  :POLE_LON = 0.0f;
  :GMT = 0.0f;
  :JULYR = 2008;
  :JULDAY = 172;
  :MAP_PROJ = 1;
  :MAP_PROJ_CHAR = "Lambert Conformal";
  :MMINLU = "USGS";
  :NUM_LAND_CAT = 24;
  :ISWATER = 16;
  :ISLAKE = -1;
  :ISICE = 24;
  :ISURBAN = 1;
  :ISOILWATER = 14;
  :history = "Thu Mar 12 10:11:06 2015: ncks -d Time,168,288 -v Times,QVAPOR,XLAT,XLONG wrfout_d01_2008-06-20_00:00:00 -o w1d1";
  :NCO = "4.0.8";
"""),
            Arguments.of("/home/snake/dev/github/netcdf/devcdm/core/src/test/data/netcdf3/testSpecialChars.nc",
                """  dimensions:
    t = 1;
    t_strlen = 43;
  variables:
    char t(t_strlen=43);
      :yow = "here is a &, <, >, \', \", \n, \r, \t, to handle";

  // global attributes:
  :omy = "here is a &, <, >, \', \", \n, \r, \t, to handle";
"""),
            )
    }

    @ParameterizedTest
    @MethodSource("params")
    fun readN3header(filename : String, expect : String) {
        println("=================")
        println(filename)
        val ncfile = Netcdf3File(filename)
        val root = ncfile.rootGroup()
         println("actual = $root")
        println("expect = $expect")

        assertEquals(expect, root.toString())
        assertTrue(root.toString().contains(expect))

        root.variables.forEach {
            println(" ${it.name} = ${it.spObject}")
        }
    }

}