

use integer;
use IO::File;
use warnings;

use Config::Any;
use Test::More;
use Test::Deep;

print "starting verify_certs_keys_data.pl\n";

our @param_file = ();
our @required_data = ();
our $param_of_interest_value = "";
our @param_of_interest_value_list = ();
our $param_value = "";
our $file_type = "";
our $val_type = "";
our $invalid_param = "false";

our $attrib_1 = "";
our $attrib_2 = "";
our $attrib_3 = "";
our $attrib_4 = "";

our %glbl_id_to_fqdn_hash = ();

our $debug = 0;
our $verbose = 0;
our $very_verbose = 0;
our $quiet = 1;

our $id_number = 0;

sub process_ID
{

    $id_number = shift;

    if ($debug) {print "line ID number is $id_number\n";}

}

sub process_open_rhs_file_name
{
    my $param_file_name = shift;
    if ($verbose) {print "param_file_name is $param_file_name\n";}

    if ( $param_file_name !~ /none/ )
    {
        open PARAMFILE, $param_file_name 
            or die "file not found\n";
    }
}


sub process_close_rhs_file_name
{
    my $param_file_name = shift;
    if ($verbose) {print "param_file_name is $param_file_name\n";}

    if ( $param_file_name !~ /none/ )
    {
        close PARAMFILE, $param_file_name
    }
}


sub process_file_type
{
    $file_type = shift;
    my $param_file_name = shift;

    if ($debug) {print "doing process_file_type\n";}
    if ( $file_type eq "f" )
    {
        if ($verbose) {print "found a flat file\n";}

        @param_file = (<PARAMFILE>);
    }

    if ( $file_type eq "x" )
    {
        if ($verbose) {print "found an xml file\n";}
###`        my $pyx_input = (<PARAMFILE>);
        my $pyx_output = `"xmlstarlet pyx $param_file_name`;
    }
}

sub process_comment_regex
{
    my $regex_pattern = shift;
    if ($debug) {print "doing process_comment_regex\n";}
    @required_data = sort ( grep { !/$regex_pattern/ } @param_file );
    if ($verbose) {print "required_data is @required_data \n";}

}

sub process_param
{
    my $param_of_interest = shift;
    chomp $param_of_interest;
    $param_of_interest =~ s/'//;
    $param_of_interest =~ s/"//;
    $param_of_interest =~ s/,//;
    $param_of_interest =~ s/;//;
    $param_of_interest =~ s/\s//;
    my $validation_type = shift;

    if ($debug) {print "doing process_param\n";}

    if ($very_verbose) {print "param_of_interest is $param_of_interest\n";}

#   ### supported delimiters are "eq" "ws" "cn" 
#   ### but not "none" because not in target file
    @param_of_interest_value_list = grep { /$param_of_interest\s|$param_of_interest.*=|$param_of_interest.*:/ } @required_data;     

    foreach $edit (@param_of_interest_value_list)
    {
        $edit =~ s/'//;
        $edit =~ s/"//;
        $edit =~ s/,//; 
        $edit =~ s/;//;
        $edit =~ s/\s//;
    }
    if ($very_verbose) {print "param_of_interest_value_list is @param_of_interest_value_list\n";}

    my $length_param_of_interest_value_list = @param_of_interest_value_list;
    if ($length_param_of_interest_value_list < 1)
    {
        if ( $validation_type ne "G" and $validation_type ne "N" )
        {
            print "FAIL:\n*****\n param missing from target config file\n";
            $invalid_param = "true";
            return 1;
        }
    }
}

sub process_val_type
{

#   ### global
    $val_type = shift;

    if ($debug) {print "doing process_val_type\n";}

}

sub process_param_attrib
{

#   ### global
    $attrib_1 = shift;
    $attrib_2 = shift;
    $attrib_3 = shift;
    $attrib_4 = shift;
    $file_name = shift;
    $rhs_separator = shift;
    my @file_name_list = ();
    if ($debug) {print "params are @param_of_interest_value_list\n";}

#   ### check whether or not the param was found in the target file
    my $length_param_of_interest_value_list = @param_of_interest_value_list; 
    if ($length_param_of_interest_value_list < 1)
    {
        if ( $val_type eq "G" or $val_type eq "N" or $val_type eq "F" or $val_type eq "B" )
        {
            push (@param_of_interest_value_list, "NotFoundFile");
            push (@param_of_interest_value_list, "Placemarker");
###            $param_of_interest_value_list[0] = qw(NotFoundFile placemarker);
        } 
        elsif ( $val_type ne "F" )
        {
            print "FAIL:\n*****\n param missing from target file\n";
            $invalid_param = "true";
            return 1;
        }
    }

### check whether a required attribute is missing
    if ( $attrib_1 eq "" )
    {
        if ( $val_type ne "F" and $val_type ne "G" and $val_type ne "N" and $val_type ne "B" )
        {
            print "ERROR:\n*****\n missing attrib in csv rules file for @param_of_interest_value_list\n";
            $invalid_param = "true";
            return 1;
        }
    }


    if ($rhs_separator eq "eq")
    {
        @file_name_list = split(/=|\s+=\s+/,$param_of_interest_value_list[0]);
    }
    elsif ($rhs_separator eq "ws")
    {
        @file_name_list = split(/\s+/,$param_of_interest_value_list[0]);
    }
    elsif ($rhs_separator eq "cn")
    {
        @file_name_list = split(/:|\s+:\s+/,$param_of_interest_value_list[0]);
    }
    elsif ($rhs_separator eq "none")
    {
        push (@file_name_list, "none");
        push (@file_name_list, "zilch");
    }
    else
    {
        print "ERROR:\n*****\n invalid separator in csv rules file for  @param_of_interest_value_list\n"; 
        return 1;
    }

    my $lhs = "";
    $lhs = $file_name_list[0];
    shift @file_name_list;
    my $rhs = "";
    $rhs = $file_name_list[0];
    if ($debug) {print "data was @param_of_interest_value_list\n";}
    if ($debug) {print "doing attrib check\n";}
    if ($verbose) {print "val type for attrib checks is $val_type\n";}

#   ### check rhs evaluates to something 
    if ( ! defined $rhs or $rhs eq "" )
    {
        if ( $val_type ne "F" and $val_type ne "G" and $val_type ne "N" and $val_type ne "B" )
        {
            print "ERROR:\n*****\n right hand side is undefined in csv rules file for @param_of_interest_value_list\n";
            $invalid_param = "true";
            return 1;
        }
    }

    if ($debug) {print "rhs is $rhs\n";}

#   ### check number of values per param
    if ( $val_type eq "Q" )
    {
        if ($verbose) {print "found val type Q\n";}
        check_quantity ($attrib_1);
    }

#   ### check the cert contents match these formats
#   ### gets path of file to check from value of param in config file
    if ( $val_type eq "C")
    {

        if ($verbose) {print "found val type C\n";}
        check_file_exists ($rhs);
        if ( $attrib_1 eq "notbag" )
        {
            check_cert_no_leading_text ($rhs);
        }
        if ( $attrib_1 eq "bag" )
        {
            check_cert_has_leading_text ($rhs);
        }
        if ( $attrib_1 eq "p12" )
        {
            check_cert_p12_format ($rhs, $attrib_1, $attrib_2);
        }
        if ( $attrib_1 eq "pem" )
        {
#           ### use x509 check for pem
            check_cert_x509_format ($rhs);
        }
        if ( $attrib_1 eq "x509" )
        {
            check_cert_x509_format ($rhs);
        }
        if ( $attrib_1 eq "jks" )
        {
            check_cert_jks_format ($rhs);
        }
    }

#   ### check the cert contents match these formats
#   ### gets path of file to check from value of param in config file
    if ( $val_type eq "CV" )
    {
        if ($verbose) {print "found val type CV\n";}

        if ( $invalid_param ne "true" )
        {
            check_cert_validity ($rhs);
        }
    }

#   ### check the key contents match these formats
#   ### gets path of file to check from value of param in config file
    if ( $val_type eq "K" )
    {
        if ($verbose) {print "found val type K\n";}
        check_file_exists ($rhs);
        if ( $attrib_1 eq "notbag" )
        {
            check_key_no_leading_text ($rhs);
        }
        if ( $attrib_1 eq "bag" )
        {
            check_key_has_leading_text ($rhs);
        }
        if ( $attrib_1 eq "p12" )
        {
            check_key_p12_format ($rhs, $attrib_1, $attrib_2);
        }
        if ( $attrib_1 eq "pem" )
        {
#           ### rsa format required for a private key
            check_key_rsa_format ($rhs, $attrib_1, $attrib_2);
        }
        if ( $attrib_1 eq "x509" )
        {
#           ### rsa format required for a private key
            check_key_rsa_format ($rhs, $attrib_1, $attrib_2);
        }
        if ( $attrib_1 eq "jks" )
        {
            check_key_jks_format ($rhs);
        }
    }

#   ### check the key contents match these formats
#   ### gets path of file to check from value of param in config file
    if ( $val_type eq "KV" )
    {
        if ($verbose) {print "found val type KV\n";}
        if ( $invalid_param ne "true" )
        {
            check_key_validity ($rhs);
        }
    }


#   ### check value of param in config file is of these formats
    if ( $val_type eq "U" )
    {
        if ($verbose) {print "found val type U\n";}
#       ### checks for https
        if ( $attrib_1 eq "secure" )
        {
            check_url_secure ($rhs);
        }
#       ### checks for http
        if ( $attrib_1 eq "insecure" )
        {
            check_url_insecure ($rhs);
        }
#       ### checks for url processed by apache
        if ( $attrib_1 eq "apache" )
        {
            check_url_apache ($rhs);
        }
#       ### checks for url processed by tomcat
        if ( $attrib_1 eq "tomcat" )
        {
            check_url_tomcat ($rhs);
        }
#       ### checks for login page url
        if ( $attrib_1 eq ( "pw" or "machine" or "federweb" or "google" or "shib" ) )
        {
            check_url_defined ($attrib_1, $rhs);
        }
#       ### checks for dns name not localhost
        if ( $attrib_1 eq "fqdn" )
        {
            check_url_fqdn ($rhs);
        }
#       ### checks for localhost not dns name
        if ( $attrib_1 eq "localhost" )
        {
            check_url_localhost ($rhs);
        }

    }

#   ### check regex matches value of param in config file
    if ( $val_type eq "R" )
    {
        if ($verbose) {print "found val type R\n";}
        check_reqex_present ($attrib_1);
    }

#   ### check regex matches value of param in config file
    if ( $val_type eq "NR" )
    {
        if ($verbose) {print "found val type NR\n";}
        check_reqex_not_present ($attrib_1);
    }

#   ### check for existence of a file path
#   ### gets path of file to check from value of param in config file
    if ( $val_type eq "F" )
    {
        if ($verbose) {print "found val type F\n";}
        check_file_exists ($rhs);
    }

    if ( $val_type eq "L" )
    {
        if ($verbose) {print "found val type L\n";}
        check_file_exists ($attrib_1);
    }

#   ### check for non-existence of a file path
    if ( $val_type eq "N" )
    {
        if ($verbose) {print "found val type N\n";}
        check_file_nonexistent ($attrib_1);
    }

#   ### check permissions on a file path
#   ### gets path of file to check from value of param in config file
    if ( $val_type eq "P" )
    {
        if ($verbose) {print "found val type P\n";}
        check_file_permissions ($rhs, $attrib_1);
    }

#   ### check number of values per param
    if ( $val_type eq "Q" )
    {
        if ($verbose) {print "found val type Q\n";}
        check_quantity ($attrib_1);
    }

#   ### check param has a boolean value
    if ( $val_type eq "B" )
    {
        if ($verbose) {print "found val type B\n";}
        check_boolean ($rhs);
    }

#   ### load globals - usually fqdn of various serveres
    if ( $val_type eq "G" )
    {
        if ($verbose) {print "found val type G\n";}
        load_global ($attrib_1, $attrib_2);
    }


}


sub check_cert
{
    if ($debug) {print "doing check_cert\n";}
}

sub check_cert_validity
{

    if ($debug) {print "doing check_cert_validity\n";}
    my $file_name = shift;
    chomp $file_name;
    $file_name =~ s/^'//;
    $file_name =~ s/'$//;

    my $ret_text =  `openssl verify $file_name 2>&1`;
    my $ret_code = $?;
    if ( $ret_code == -1 )
    {
        print "FAIL:\n*****\n error -1 at check_cert_validity for $file_name in @param_of_interest_value_list - command failed: $!\n";
        $invalid_param = "true";
        return 1;
    }
    else
    {
        if ( $ret_code == 0 )
        {
            if ( $ret_text =~ /: OK/ )
            {
                if ( ! $quiet ) {print "OK: at check_cert_validity for $file_name in @param_of_interest_value_list\n";}
            }
            else
            {
                printf "FAIL:\n*****\n error from x509 at check_cert_validity for $file_name in @param_of_interest_value_list - command exited with value %d", $ret_code >> 8, "\n";
                $invalid_param = "true";
                return 1;
            }
        }
        else
        {
            printf "FAIL:\n*****\n error from x509 at check_cert_validity for $file_name in @param_of_interest_value_list - command exited with value %d", $ret_code >> 8, "\n";
            $invalid_param = "true";
            return 1;
        }
    }
}

sub check_key
{
    if ($debug) {print "doing check_key\n";}


}
sub check_key_validity
{

    if ($debug) {print "doing check_key_validity\n";}
    my $file_name = shift;
    chomp $file_name;
    $file_name =~ s/^'//;
    $file_name =~ s/'$//;

    my $ret_text = `openssl rsa -check -noout -in $file_name 2>&1`;
    my $ret_code = $?;
    if ( $ret_code == -1 )
    {
        print "FAIL:\n*****\n error -1 at check_key_validity for $file_name in @param_of_interest_value_list - command failed: $!\n";
        $invalid_param = "true";
        return 1;
    }
    else
    {
        if ( $ret_code == 0 )
        {
            if ( $ret_text =~ /RSA key ok/ )
            {
                if ( ! $quiet ) {print "OK: at check_key_validity for $file_name in @param_of_interest_value_list\n";}
            }
            else
            {
                printf "FAIL:\n*****\n error from rsa return text at check_key_validity for $file_name in @param_of_interest_value_list - command exited with value %d", $ret_code >> 8 , "\n";
                $invalid_param = "true";
                return 1;
            }
        }
        else
        {
            printf "FAIL:\n*****\n error from rsa at check_key_validity for $file_name in @param_of_interest_value_list - command exited with value %d", $ret_code >> 8 , "\n";
            $invalid_param = "true";
            return 1;
        }
    }
}

sub check_url
{

    if ($debug) {print "doing check_url\n";}
    my $server = shift;
    chomp $server;

### lookup dns to check the url exists
    system("nslookup $server | grep $server | awk '{print $2}'");
    if ( $? == -1 )
    {
        print "FAIL:\n*****\n error -1 for nslookup at check_url for $file_name in @param_of_interest_value_list - command failed: $!\n";
        $invalid_param = "true";
        return 1;
    }
    else
    {
        if ( $? == 0 )
        {
            if ( ! $quiet ) {print "OK: at check_url for $file_name in @param_of_interest_value_list\n";}
        }
        else
        {
            printf "FAIL:\n*****\n error from nslookup at check_url for $file_name in @param_of_interest_value_list - command exited with value %d", $? >> 8 , "\n";
            $invalid_param = "true";
            return 1;
        }
    }

### ping the url to check the ip responds
    system("ping -q -c 1 $server  >/dev/null 2>&1");
    if ( $? == -1 )
    {
        print "FAIL:\n*****\n error -1 for ping at check_url for $file_name in @param_of_interest_value_list - command failed: $!\n";
        $invalid_param = "true";
        return 1;
    }
    else
    {
        if ( $? == 0 )
        {
            if ( ! $quiet ) {print "OK: at check_url for $file_name in @param_of_interest_value_list\n";}
        }
        else
        {
            printf "FAIL:\n*****\n error from ping at check_url for $file_name in @param_of_interest_value_list - command exited with value %d", $? >> 8 , "\n";
            $invalid_param = "true";
            return 1;
        }
    }


### grab the webpage to see whether its alive

#    curl -get -s --show-error https://$page 2>&1 | cat - > output_page.txt
#    page_found=`grep -c -P "^\[\]$|^\[.*\]$" output_page.txt`

}

sub check_cert_no_leading_text
{
    if ($debug) {print "doing check_cert_no_leading_text\n";}
    my @certfile_array = ();
    my $cert_location = shift;
    chomp $cert_location;
    $cert_location =~ s/^'//;
    $cert_location =~ s/'$//;

    if ($verbose) {print "cert_location is $cert_location\n";}
###    open CERTFILE, $cert_location;
    open CERTFILE, $cert_location 
        or do {
        print "FAIL:\n*****\n error $? - unable to open file - at check_cert_no_leading_text for $cert_location in @param_of_interest_value_list\n";
        $invalid_param = "true";
        return 1;
    };
    @certfile_array = <CERTFILE>;
    if ( $certfile_array[0] !~ /-----BEGIN CERTIFICATE-----/ )
    {
        print "FAIL:\n*****\n begin certificate not found on line 1 - at check_cert_no_leading_text for $certfile_array[0] in @param_of_interest_value_list\n";
        $invalid_param = "true";
        return 1;
    }

#   ### check there is no text after END CERT line
    if ( $certfile_array[-1] !~ /-----END CERTIFICATE-----/ )
    {
        print "FAIL:\n*****\n end certificate not found on last line - at check_cert_no_leading_text for $certfile_array[-1] in @param_of_interest_value_list\n";
        $invalid_param = "true";
        return 1;
    }

#   ### check there is no text between END CERT and following BEGIN CERT
#   ### read from line 2
    for ( $i=0 + 1 ; $i <= $#certfile_array ; $i++ )
    {
        if ( $certfile_array[$i] =~ /-----BEGIN CERTIFICATE-----/ )
        {
            if ( $certfile_array[$i - 1] !~ /-----END CERTIFICATE-----/ )
            {
                print "FAIL:\n*****\n end certificate does not precede begin certificate in mid file - at check_cert_no_leading_text for $certfile_array[$i - 1] in @param_of_interest_value_list\n";
                $invalid_param = "true";
                return 1;
            }
        }
    }

#   ### multiple certificates in one file are allowed by this check


    close CERTFILE;


    if ( ! $quiet ) {print "OK: at check_cert_no_leading_text for $cert_location in @param_of_interest_value_list\n";}


}

sub check_cert_has_leading_text
{
    if ($debug) {print "doing check_cert_has_leading_text\n";}
    my @certfile_array = ();
    my $cert_location = shift;
    chomp $cert_location;
    $cert_location =~ s/^'//;
    $cert_location =~ s/'$//;

    if ($verbose) {print "cert_location is $cert_location\n";}
    open CERTFILE, $cert_location
        or do {
        print "FAIL:\n*****\n error $? - unable to open file - at check_cert_has_leading_text for $cert_location in @param_of_interest_value_list\n";
        $invalid_param = "true";
        return 1;
    };
    @certfile_array = <CERTFILE>;
    if ( $certfile_array[0] =~ /-----BEGIN CERTIFICATE-----/ )
    {
        print "FAIL:\n*****\n begin certificate found on line 1 - at check_cert_has_leading_text for $certfile_array[0] in @param_of_interest_value_list\n";
        $invalid_param = "true";
        return 1;
    }


#   ### check there is some text between END CERT and following BEGIN CERT
#   ### read from line 2
    for ( $i=0 + 1 ; $i <= $#certfile_array ; $i++ )
    {
        if ( $certfile_array[$i] =~ /-----BEGIN CERTIFICATE-----/ )
        {
            if ( $certfile_array[$i - 1] =~ /-----END CERTIFICATE-----/ )
            {
                print "FAIL:\n*****\n end certificate immediately precedes begin certificate in mid file - at check_cert_has_leading_text for $certfile_array[$i - 1] in @param_of_interest_value_list\n";
                $invalid_param = "true";
                return 1;
            }
        }
    }

#   ### multiple certificates in one file are allowed by this check


    close CERTFILE;


    if ( ! $quiet ) {print "OK: at check_cert_has_leading_text for $cert_location in @param_of_interest_value_list\n";}

}


sub check_cert_p12_format
{
    if ($debug) {print "doing check_cert_p12_format\n";}
    my $cert_location = shift;
    my $password_read = shift;
    my $password_export = shift;

    if ( ! defined $password_read or $password_read = '' )
    {
        printf "FAIL:\n*****\n error from jks validation at check_cert_jks_format for $cert_location in @param_of_interest_value_list `n";
        $invalid_param = "true";
        return 1;
    }
    if ( ! defined $password_export or $password_export = '' )
    {
        printf "FAIL:\n*****\n error from jks validation at check_cert_jks_format for $cert_location in @param_of_interest_value_list `n";
        $invalid_param = "true";
        return 1;
    }

    chomp $cert_location;
    $cert_location =~ s/^'//;
    $cert_location =~ s/'$//;
    chomp $password_read;
    chomp $password_export;

    my $ret_text = `openssl pkcs12 -info -in $cert_location -passout pass:$password_export -password pass:$password_read 2>&1`;
    my $ret_code = $?;
    if ( $ret_code == -1 )
    {
        print "FAIL:\n*****\n error -1 for openssl pkcs12 validation at check_cert_p12_format for $cert_location in @param_of_interest_value_list - command failed: $!\n";
        $invalid_param = "true";
        return 1;
    }
    else
    {
        if ( $ret_code == 0 )
        {
            if ( ! $quiet ) {print "OK: at check_cert_p12_format for $cert_location in @param_of_interest_value_list\n";}
        }
        else
        {
            printf "FAIL:\n*****\n error from openssl pkcs12 validation at check_cert_p12_format for $cert_location in @param_of_interest_value_list - command exited with value %d", $ret_code >> 8 , "\n";
            $invalid_param = "true";
            return 1;
        }
    }


}

sub check_cert_x509_format
{
    if ($debug) {print "doing check_cert_x509_format\n";}
    my $cert_location = shift;
    chomp $cert_location;
    $cert_location =~ s/^'//;
    $cert_location =~ s/'$//;

    my $ret_text =  `openssl verify $cert_location 2>&1`;
    my $ret_code = $?;
    if ( $ret_code == -1 )
    {
        print "FAIL:\n*****\n error -1 at check_cert_x509_format for $cert_location in @param_of_interest_value_list - command failed: $!\n";
        $invalid_param = "true";
        return 1;
    }
    else
    {
        if ( $ret_code == 0 )
        {
            if ( $ret_text =~ /: OK/ )
            {
                if ( ! $quiet ) {print "OK: at check_cert_x509_format for $cert_location in @param_of_interest_value_list\n";}
            }
            else
            {
                printf "FAIL:\n*****\n error from x509 at check_cert_x509_format for $cert_location in @param_of_interest_value_list - command exited with value %d", $ret_code >> 8, "\n";
                $invalid_param = "true";
                return 1;
            }
        }
        else
        {
            printf "FAIL:\n*****\n error from x509 at check_cert_x509_format for $cert_location in @param_of_interest_value_list - command exited with value %d", $ret_code >> 8, "\n";
            $invalid_param = "true";
            return 1;
        }
    }
}

sub check_cert_jks_format
{
    if ($debug) {print "doing check_cert_jks_format\n";}
    my $cert_location = shift;
    my $password_read = shift;
    my $password_export = shift;

    if ( ! defined $password_read or $password_read = '' ) 
    {
        printf "FAIL:\n*****\n error from jks validation at check_cert_jks_format for $cert_location in @param_of_interest_value_list `n";
        $invalid_param = "true";
        return 1;
    }
    if ( ! defined $password_export or $password_export = '' )
    {
        printf "FAIL:\n*****\n error from jks validation at check_cert_jks_format for $cert_location in @param_of_interest_value_list `n";
        $invalid_param = "true";
        return 1;
    }

    chomp $cert_location;
    $cert_location =~ s/^'//;
    $cert_location =~ s/'$//;
    chomp $password_read;
    chomp $password_export;

    my $ret_text = `keytool -list -keystore $cert_location >/dev/null 2>&1`;
    my $ret_code = $?;
    if ( $ret_code == -1 )
    {
        print "FAIL:\n*****\n error -1 for jks validation at check_cert_jks_format for $cert_location in @param_of_interest_value_list - command failed: $!\n";
        $invalid_param = "true";
        return 1;
    }
    else
    {
        if ( $ret_code == 0 )
        {
            if ( $ret_text !~ /^keytool error:/ )
            {
                if ( ! $quiet ) {print "OK: at check_cert_jks_format for $cert_location in @param_of_interest_value_list\n";}
            }
            else
            {
                printf "FAIL:\n*****\n error from jks validation at check_cert_jks_format for $cert_location in @param_of_interest_value_list - command exited with value %d", $ret_code >> 8 , "\n";
            $invalid_param = "true";
            return 1;
            }
        }
        else
        {
            printf "FAIL:\n*****\n error from jks validation at check_cert_jks_format for $cert_location in @param_of_interest_value_list - command exited with value %d", $ret_code >> 8 , "\n";
            $invalid_param = "true";
            return 1;
        }
    }

}


sub check_key_no_leading_text
{
    if ($debug) {print "doing check_key_no_leading_text\n";}
    my @keyfile_array = ();
    my $key_location = shift;
    chomp $key_location;
    $key_location =~ s/^'//;
    $key_location =~ s/'$//;

    if ($verbose) {print "key_location is $key_location\n";}
    open KEYFILE, $key_location
        or do {
        print "FAIL:\n*****\n error $? - unable to open file - at check_key_no_leading_text for $key_location in @param_of_interest_value_list\n";
        $invalid_param = "true";
        return 1;
    };
    @keyfile_array = <KEYFILE>;
    if ( $keyfile_array[0] !~ /-----BEGIN PRIVATE KEY-----/ )
    {
        print "FAIL:\n*****\n begin private key not found on line 1 - at check_key_no_leading_text for $keyfile_array[0] in @param_of_interest_value_list\n";
        $invalid_param = "true";
        return 1;
    }

#   ### check there is no text after END PRV KEY line
    if ( $keyfile_array[-1] !~ /-----END PRIVATE KEY-----/ )
    {
        print "FAIL:\n*****\n end private key not found on last line - at check_key_no_leading_text for $keyfile_array[-1] in @param_of_interest_value_list\n";
        $invalid_param = "true";
        return 1;
    }
#   ### check there is no text between END PRV KEY and following BEGIN PRV KEY
#   ### read from line 2
    for ( $i=0 + 1 ; $i <= $#keyfile_array ; $i++ )
    {
        if ( $keyfile_array[$i] =~ /-----BEGIN RSA PRIVATE KEY-----/ )
        {
            if ( $keyfile_array[$i - 1] !~ /-----END RSA PRIVATE KEY-----/ )
            {
                print "FAIL:\n*****\n end private key does not precede begin private key in mid file - at check_key_no_leading_text for $keyfile_array[$i - 1] in @param_of_interest_value_list\n";
                $invalid_param = "true";
                return 1;
            }
        }
    }

#   ### multiple certificates in one file are allowed by this check


    close KEYFILE;


    if ( ! $quiet ) {print "OK: at check_key_no_leading_text for $key_location in @param_of_interest_value_list\n";}


}

sub check_key_has_leading_text
{
    if ($debug) {print "doing check_key_has_leading_text\n";}
    my @keyfile_array = ();
    my $key_location = shift;
    chomp $key_location;
    $key_location =~ s/^'//;
    $key_location =~ s/'$//;
    if ($verbose) {print "key_location is $key_location\n";}
    open KEYFILE, $key_location
        or do {
        print "FAIL:\n*****\n error $? - unable to open file - at check_key_has_leading_text for $key_location in @param_of_interest_value_list\n";
        $invalid_param = "true";
        return 1;
    };
    @keyfile_array = <KEYFILE>;
    if ( $keyfile_array[0] =~ /-----BEGIN RSA PRIVATE KEY-----/ )
    {
        print "FAIL:\n*****\n begin private key found on line 1 - at check_key_has_leading_text for $keyfile_array[0] in @param_of_interest_value_list\n";
        $invalid_param = "true";
        return 1;
    }


#   ### check there is some text between END PRV KEY and following BEGIN PRV KEY
#   ### read from line 2
    for ( $i=0 + 1 ; $i <= $#keyfile_array ; $i++ )
    {
        if ( $keyfile_array[$i] =~ /-----BEGIN RSA PRIVATE KEY-----/ )
        {
            if ( $keyfile_array[$i - 1] =~ /-----END RSA PRIVATE KEY-----/ )
            {
                print "FAIL:\n*****\n end private key immediately precedes begin private key in mid file - at check_key_has_leading_text for $keyfile_array[$i - 1] in @param_of_interest_value_list\n";
                $invalid_param = "true";
                return 1;
            }
        }
    }

#   ### multiple certificates in one file are allowed by this check


    close KEYFILE;


    if ( ! $quiet ) {print "OK: at check_key_has_leading_text for $key_location in @param_of_interest_value_list\n";}

}


sub check_key_p12_format
{
    if ($debug) {print "doing check_key_p12_format\n";}
    my $key_location = shift;
    my $password_read = shift;
    my $password_export = shift;

    if ( ! defined $password_read or $password_read = '' )
    {
        printf "FAIL:\n*****\n error from jks validation at check_cert_jks_format for $cert_location in @param_of_interest_value_list `n";
        $invalid_param = "true";
        return 1;
    }
    if ( ! defined $password_export or $password_export = '' )
    {
        printf "FAIL:\n*****\n error from jks validation at check_cert_jks_format for $cert_location in @param_of_interest_value_list `n";
        $invalid_param = "true";
        return 1;
    }

    chomp $key_location;
    $key_location =~ s/^'//;
    $key_location =~ s/'$//;
    chomp $password_read;
    chomp $password_export;

    my $ret_text = `openssl pkcs12 -info -in $key_location -passout pass:$password_export -password pass:$password_read >/dev/null 2>&1`;
    my $ret_code = $?;
    if ( $ret_code == -1 )
    {
        print "FAIL:\n*****\n error -1 for openssl pkcs12 validation at check_key_p12_format for $key_location in @param_of_interest_value_list - command failed: $!\n";
        $invalid_param = "true";
        return 1;
    }
    else
    {
        if ( $ret_code == 0 )
        {
            if ( ! $quiet ) {print "OK: at check_key_p12_format for $key_location in @param_of_interest_value_list\n";}
        }
        else
        {
            printf "FAIL:\n*****\n error from openssl pkcs12 validation at check_key_p12_format for $key_location in @param_of_interest_value_list - command exited with value %d", $ret_code >> 8 , "\n";
            $invalid_param = "true";
            return 1;
        }
    }

}

sub check_key_rsa_format
{
    if ($debug) {print "doing check_key_rsa_format\n";}
    my $file_name = shift;
    chomp $file_name;
    $file_name =~ s/^'//;
    $file_name =~ s/'$//;

    my $ret_text = `openssl rsa -in $file_name -check 2>&1`;
    my $ret_code = $?;
    if ( $ret_code == -1 )
    {
        print "FAIL:\n*****\n error -1 at check_key_rsa_format for $file_name in @param_of_interest_value_list - command failed: $!\n";
        $invalid_param = "true";
        return 1;
    }
    else
    {
        if ( $ret_code == 0 )
        {
            if ( $ret_text eq "RSA key ok" )
            {
                if ( ! $quiet ) {print "OK: at check_key_rsa_format for $file_name in @param_of_interest_value_list\n";}
            }
            else
            {
                printf "FAIL:\n*****\n error from rsa at check_key_rsa_format for $file_name in @param_of_interest_value_list - command exited with value %d", $ret_code >> 8 , "\n";
                $invalid_param = "true";
                return 1;
            }

        }
        else
        {
            printf "FAIL:\n*****\n error from rsa at check_key_rsa_format for $file_name in @param_of_interest_value_list - command exited with value %d", $ret_code >> 8 , "\n";
            $invalid_param = "true";
            return 1;
        }
    }

}

sub check_key_jks_format
{
    if ($debug) {print "doing check_key_jks_format\n";}
    my $key_location = shift;
    my $password_read = shift;
    my $password_export = shift;

    if ( ! defined $password_read or $password_read = '' )
    {
        printf "FAIL:\n*****\n error from jks validation at check_cert_jks_format for $cert_location in @param_of_interest_value_list `n";
        $invalid_param = "true";
        return 1;
    }
    if ( ! defined $password_export or $password_export = '' )
    {
        printf "FAIL:\n*****\n error from jks validation at check_cert_jks_format for $cert_location in @param_of_interest_value_list `n";
        $invalid_param = "true";
        return 1;
    }

    chomp $key_location;
    $key_location =~ s/^'//;
    $key_location =~ s/'$//;
    chomp $password_read;
    chomp $password_export;

    my $ret_text = `keytool -list -keystore $key_location >/dev/null 2>&1`;
    my $ret_code = $?;
    if ( $ret_code == -1 )
    {
        print "FAIL:\n*****\n error -1 for jks validation at check_key_jks_format for $key_location in @param_of_interest_value_list - command failed: $!\n";
        $invalid_param = "true";
        return 1;
    }
    else
    {
        if ( $ret_code == 0 )
        {
            if ( $ret_text !~ /^keytool error:/ )
            {
                if ( ! $quiet ) {print "OK: at check_key_jks_format for $key_location in @param_of_interest_value_list\n";}
            }
            else
            {
                printf "FAIL:\n*****\n error from jks validation at check_key_jks_format for $key_location in @param_of_interest_value_list - command exited with value %d", $ret_code >> 8 , "\n";
                $invalid_param = "true";
                return 1;
            }
        }
        else
        {
            printf "FAIL:\n*****\n error from jks validation at check_key_jks_format for $key_location in @param_of_interest_value_list - command exited with value %d", $ret_code >> 8 , "\n";
            $invalid_param = "true";
            return 1;
        }
    }

}
sub check_reqex_present
{

    if ($debug) {print "doing check_reqex_present\n";}
    my $srch = shift; 
    if ($debug) {print "srch is $srch\n";}
    if ($debug) {print "param_of_interest_value_list is @param_of_interest_value_list\n";}
    my @result = grep { /$srch/ } @param_of_interest_value_list;
###    my @result = grep { /$attrib_1\s|$attrib_1=|$attrib_1$|$attrib_1\'/ } @param_of_interest_value_list;
    if ( ! @result )
    {
        print "FAIL:\n*****\n at check_reqex_present for $attrib_1 in @param_of_interest_value_list\n";
        $invalid_param = "true";
        return 1;
    }
    if ( ! $quiet ) {print "OK:  at check_reqex_present for $attrib_1 in @param_of_interest_value_list\n";}

}

sub check_reqex_not_present
{

    if ($debug) {print "doing check_reqex_not_present\n";}
    my $srch = shift;
    my @result = grep { /$srch/ } @param_of_interest_value_list;
###    my @result = grep { /$attrib_1\s|$attrib_1=|$attrib_1$|$attrib_1\'/ } @param_of_interest_value_list;

    if ( @result )
    {
        print "FAIL:\n*****\n at check_reqex_not_present for $attrib_1 in @param_of_interest_value_list\n";
        $invalid_param = "true";
        return 1;
    }
    if ( ! $quiet ) {print "OK:  at check_reqex_not_present for $attrib_1 in @param_of_interest_value_list\n";}

}


sub check_file_exists
{
    if ($debug) {print "doing check_file_exists\n";}
    my $file_name = shift;
    $file_name =~ s/^'//;
    $file_name =~ s/'$//;
    chomp $file_name;

    if (-e "$file_name") {
        if ( ! $quiet ) {print "OK:  at check_file_exists for $file_name in @param_of_interest_value_list\n";}
    } 
    else
    {
        if ( -d "$file_name") {
            if ( ! $quiet ) {print "OK:  at check_file_exists for $file_name in @param_of_interest_value_list\n";}
        }
        else
        {
            print "FAIL:\n*****\n at check_file_exists for $file_name in @param_of_interest_value_list\n";
            $invalid_param = "true";
            return 1;
        }
    }
}


sub check_file_nonexistent
{
    if ($debug) {print "doing check_file_nonexistent\n";}
    my $file_name = shift;
    $file_name =~ s/^'//;
    $file_name =~ s/'$//;
    chomp $file_name;

    if ( ! -f "$file_name") {
        if ( ! $quiet ) {print "OK:  at check_file_nonexistent for $file_name in @param_of_interest_value_list\n";}
    }
    elsif ( ! -d "$file_name") 
    {
        if ( ! $quiet ) {print "OK:  at check_file_nonexistent for $file_name in @param_of_interest_value_list\n";}
    }
    else
    {
        print "FAIL:\n*****\n at check_file_nonexistent for $file_name in @param_of_interest_value_list\n";
        $invalid_param = "true";
        return 1;
    }
}

sub check_file_permissions
{
    if ($debug) {print "doing check_file_permissions\n";}
    my $file_name = shift;
    chomp $file_name;
    $file_name =~ s/^'//;
    $file_name =~ s/'$//;
    my $perms = shift;
    chomp $perms;

    my $mode = (stat($file_name))[2];
    my $permissions = eval(printf "Permissions are %04o\n", $mode & 07777);
    if ( $permissions eq $perms )
    {
        if ( ! $quiet ) {print "OK:  at check_file_permissions for $file_name in @param_of_interest_value_list\n";}
    }
    else
    {
        print "FAIL:\n*****\n at check_file_permissions for $file_name in @param_of_interest_value_list\n";
        $invalid_param = "true";
        return 1;
    }
}

sub check_quantity
{
    if ($debug) {print "doing check_quantity\n";}
    my $qty = shift;
    my $length_param_of_interest_value_list = @param_of_interest_value_list;
    if ($debug) {print "length of array is $length_param_of_interest_value_list\n";}
    if ($debug) {print "data for array is @param_of_interest_value_list";}
    if ( $length_param_of_interest_value_list > $qty )
    {
        print "FAIL:\n*****\n at check_quantity too many for $qty versus $length_param_of_interest_value_list in @param_of_interest_value_list\n";
        $invalid_param = "true";
        return 1;
    }
    elsif ( $length_param_of_interest_value_list < $qty ) 
    {
        print "FAIL:\n*****\n at check_quantity too few for $qty versus $length_param_of_interest_value_list in @param_of_interest_value_list\n";
        $invalid_param = "true";
        return 1;
    }
    if ( ! $quiet ) {print "OK: at check_quantity for $qty versus $length_param_of_interest_value_list\n";}

}


sub check_boolean
{
    if ($debug) {print "doing check_boolean\n";}
    my $truth_hood = shift;
    if ( $truth_hood !~ /True|true|TRUE|False|false|FALSE|1|0/ )
    {
        print "FAIL:\n*****\n at check_boolean for $truth_hood in @param_of_interest_value_list\n";
        $invalid_param = "true";
        return 1;
    }
    if ( ! $quiet ) {print "OK: at check_boolean for $truth_hood in @param_of_interest_value_list\n";}

}

sub check_url_secure
{
    if ($debug) {print "doing check_url_secure\n";}
    my $web_addr = shift;
    if ( $web_addr !~ /https:\/\// )
    {
        print "FAIL:\n*****\n at check_url_secure for $web_addr in @param_of_interest_value_list\n";
        $invalid_param = "true";
        return 1;
    }
    if ( ! $quiet ) {print "OK: at check_url_secure for $web_addr in @param_of_interest_value_list\n";}
}

sub check_url_insecure
{
    if ($debug) {print "doing check_url_insecure\n";}
    my $web_addr = shift;
    if ( $web_addr !~ /http:\/\// )
    {
        print "FAIL:\n*****\n at check_url_insecure for $web_addr in @param_of_interest_value_list\n";
        $invalid_param = "true";
        return 1;
    }
    if ( ! $quiet ) {print "OK: at check_url_insecure for $web_addr in @param_of_interest_value_list\n";}

}

sub check_url_apache
{
    if ($debug) {print "doing check_url_apache\n";}
    my $web_addr = shift;
### this is WRONG
    if ( ( $web_addr !~ /:/ or $web_addr =~ /:80$|:443$/ ) and $web_addr !~ /:8443$|:8080$/ )
    {
        print "FAIL:\n*****\n at check_apache for $web_addr in @param_of_interest_value_list\n";
        $invalid_param = "true";
        return 1;
    }
    if ( ! $quiet ) {print "OK: at check_apache for $web_addr in @param_of_interest_value_list\n";}

}

sub check_url_tomcat
{
    if ($debug) {print "doing check_url_tomcat\n";}
    my $web_addr = shift;
    if ( $web_addr !~ /:8443|:8080/ )
    {
        print "FAIL:\n*****\n at check_tomcat for $web_addr in @param_of_interest_value_list\n";
        $invalid_param = "true";
        return 1;
    }
    if ( ! $quiet ) {print "OK: at check_tomcat for $web_addr in @param_of_interest_value_list\n";}

}

sub check_url_defined
{
    if ($debug) {print "doing check_url_defined\n";}
    my $web_addr = shift;
    my $rhs = shift;

    my $not_found = "true";
    while (($id_of_fqdn, $fqdn) = each(%glbl_id_to_fqdn_hash)){
        if ( $id_of_fqdn eq $web_addr and $fqdn = $rhs )
        {
            $not_found = "false";
            if ( ! $quiet ) {print "OK:  at check_url_defined for $web_addr matches $rhs in @param_of_interest_value_list\n";}
        }
    }

    if ( $not_found eq "true" ) 
    {
        print "FAIL:\n*****\n at check_url_defined for $web_addr in @param_of_interest_value_list\n";
        $invalid_param = "true";
        return 1;
    }
}
###sub check_url_machine
###{
###    if ($debug) {print "doing check_url_machine\n";}
###    my $web_addr = shift;
###
###}
###sub check_url_federweb
###{
###    if ($debug) {print "doing check_url_federweb\n";}
###    my $web_addr = shift;
###
###
###}
###sub check_url_google_simplesaml
###{
###    if ($debug) {print "doing check_url_google_simplesaml\n";}
###    my $web_addr = shift;
###
###
###}
###sub check_url_shib_simplesaml
###{
###    if ($debug) {print "doing check_url_shib_simplesaml\n";}
###    my $web_addr = shift;
###
###}
sub check_url_fqdn
{
    if ($debug) {print "doing check_url_fqdn\n";}
    my $web_addr = shift;

#   ### check this is not localhost
    if ( $web_addr =~ /localhost/ )
    {
        print "FAIL:\n*****\n at check_url_fqdn for $web_addr in @param_of_interest_value_list\n";
        $invalid_param = "true";
        return 1;
    }
    if ( ! $quiet ) {print "OK:  at check_url_fqdn for $web_addr in @param_of_interest_value_list\n";}

}
sub check_url_localhost
{
    if ($debug) {print "doing check_url_localhost\n";}
    my $web_addr = shift;

#   ### check this is localhost
    if ( $web_addr !~ /localhost/ and $web_addr !~ /0.0.0.0/ )
    {
        print "FAIL:\n*****\n at check_url_localhost for $web_addr in @param_of_interest_value_list\n";
        $invalid_param = "true";
        return 1;
    }
    if ( ! $quiet ) {print "OK:  at check_url_localhost for $web_addr in @param_of_interest_value_list\n";}

}

sub load_global
{
    if ($debug) {print "doing load_global\n";}
    my $glbl_name = shift;
    my $glbl_value = shift;

    $glbl_id_to_fqdn_hash{$glbl_name} = $glbl_value;

}

sub process_param_attrib_2
{

    my $attrib_2 = shift;
    if ($debug) {print "doing attrib_2 check\n";}

    if ( $attrib_2 eq "" )
    {
        print "nothing to do\n";
        next
    }

}

sub process_param_attrib_3
{

    my $attrib_3 = shift;
    if ($debug) {print "doing attrib_3 check\n";}

    if ( $attrib_3 eq "" )
    {
        print "nothing to do\n";
        next
    }

}

sub process_param_attrib_4
{

    my $attrib_4 = shift;
    if ($debug) {print "doing attrib_4 check\n";}

    if ( $attrib_4 eq "" )
    {
        print "nothing to do\n";
        next
    }

}

###########################################################################
###                          Main                                       ###
###########################################################################

###open INFILE, 'verify_certs_keys_data.csv'
###open INFILE, 'verify_certs_keys_data.TEST01.csv'
###	or die "unable to open rules file\n";
open INFILE, 'verify_certs_keys_data.csv'
        or die "unable to open rules file\n";


my $old_param = "dummy";
### loop thru all the lines of the file
my $line_count = 0;
while (<INFILE>)
{

    $line_count++;

    @rules_list = split(/,/,$_);
    chomp(@rules_list);
    if ($verbose) {print "rules_list is @rules_list\n";}

#   ### first line is column headers so discard
    next if $line_count == 1 ;

    my $bad_line = "false";
#   ### loop thru the columns on each line of data
    for ( $i = 0; $i <= 10; ++$i )
    {
        my $stuff = "";
        $stuff = $rules_list[$i];
        if ($debug) {print "invalid_param is $invalid_param\n";}

#       ### REFACTOR: just put the known subscripts in place of i
        if ( $i == 0 )
        {
            process_ID $rules_list[$i];
        }

        if ( $i == 1 )
        {
            process_open_rhs_file_name $rules_list[$i];
        }

        if ( $i == 2 )
        {
            process_file_type $rules_list[$i], $rules_list[1];
        }
        if ( $i == 3 )
        {
            process_comment_regex $rules_list[$i];
        }
        if ( $i == 4 )
        {
            process_param $rules_list[$i], $rules_list[5];
        }
        if ( $i == 5 )
        {
            process_val_type $rules_list[$i];
        }

        if ( $bad_line eq "false" )
        {
            if ( $i == 7 )
            {
                process_param_attrib $rules_list[$i], $rules_list[$i + 1], $rules_list[$i + 2], $rules_list[$i + 3], $rules_list[1], $rules_list[6];
            }
        }

        if ($debug) {print "invalid_param is $invalid_param\n";}
        if ( $invalid_param eq "true" )
        {
            $invalid_param = "false";
            $bad_line = "true";
            last;
        }


    }

    if ( $bad_line eq "true" )
    {
        if ( ! $quiet )
        {
            $bad_line = "false";
            print "A failure was detected for rule @rules_list\n";
            print ".....................................................\n";
            print "\n";
        }
     }

    process_close_rhs_file_name $rules_list[1];

}

close INFILE;
print "normal end of prog\n";
