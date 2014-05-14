<?php



if(!empty($this->data['htmlinject']['htmlContentPost'])) {
	foreach($this->data['htmlinject']['htmlContentPost'] AS $c) {
		echo $c;
	}
}


?>



		<hr />

		<img src="/<?php echo $this->data['baseurlpath']; ?>resources/contrail-theme/img/logo_small.png" width="100" alt="Contrail logo" style="float: right" />		
		<center>Contrail Project 2013 <a href="http://contrail-project.eu">Contrail Project</a></center>
		
		<br style="clear: right" />
	
	</div><!-- #content -->

</div><!-- #wrap -->

</body>
</html>
