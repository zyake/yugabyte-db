/*
 * Copyright 2022 YugaByte, Inc. and Contributors
 *
 * Licensed under the Polyform Free Trial License 1.0.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 *
 * https://github.com/YugaByte/yugabyte-db/blob/master/licenses/
 *  POLYFORM-FREE-TRIAL-LICENSE-1.0.0.txt
 */

package com.yugabyte.yw.common.certmgmt.providers;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.google.api.client.util.Strings;
import com.yugabyte.yw.common.certmgmt.CertConfigType;
import com.yugabyte.yw.common.certmgmt.CertificateDetails;
import com.yugabyte.yw.common.certmgmt.CertificateHelper;
import com.yugabyte.yw.common.kms.util.hashicorpvault.HashicorpVaultConfigParams;
import com.yugabyte.yw.common.kms.util.hashicorpvault.VaultAccessor;
import com.yugabyte.yw.models.CertificateInfo;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.ebean.annotation.EnumValue;

/*
 * Wrapper over Vault.logical for PKI operations
 */
public class VaultPKI extends CertificateProviderBase {
  public static final Logger LOG = LoggerFactory.getLogger(VaultPKI.class);

  public static String ISSUE_FIELD_CERT = "certificate";
  public static String ISSUE_FIELD_PRV_KEY = "private_key";
  public static String ISSUE_FIELD_CA = "issuing_ca";
  public static String ISSUE_FIELD_CA_CHAIN = "ca_chain";
  public static String ISSUE_FIELD_SERIAL = "serial_number";

  public enum VaultOperationsForCert {
    @EnumValue("issue")
    ISSUE,

    @EnumValue("cert")
    CERT,

    @EnumValue("cacert")
    CA_CERT,

    @EnumValue("ca_chain")
    CA_CHAIN,

    @EnumValue("crl")
    CRL,

    @EnumValue("roles")
    ROLES;

    public String toString() {
      switch (this) {
        case ISSUE:
          return "issue";
        case CA_CERT:
          return "ca";
        case CA_CHAIN:
          return "ca_chain";
        case CERT:
          return "cert";
        case CRL:
          return "crl";
        case ROLES:
          return "roles";
        default:
          return null;
      }
    }
  }

  HashicorpVaultConfigParams params;
  VaultAccessor vAccessor;

  private String curCaCertificateStr;
  private String curCertificateStr;
  private String curKeyStr;

  public VaultPKI(UUID pCACertUUID) {
    super(CertConfigType.HashicorpVault, pCACertUUID);
    curCaCertificateStr = "";
    curCertificateStr = "";
    curKeyStr = "";
  }

  public VaultPKI(UUID pCACertUUID, VaultAccessor vObj, HashicorpVaultConfigParams configInfo) {
    super(CertConfigType.HashicorpVault, pCACertUUID);
    vAccessor = vObj;
    params = configInfo;

    curCaCertificateStr = "";
    curCertificateStr = "";
    curKeyStr = "";
  }

  public static VaultPKI getVaultPKIInstance(CertificateInfo caCertConfigInfo) throws Exception {
    // LOG.info("Called from: {}", CommonUtils.getStackTraceHere());

    HashicorpVaultConfigParams hcConfigInfo = caCertConfigInfo.getCustomHCPKICertInfoInternal();
    return getVaultPKIInstance(caCertConfigInfo.uuid, hcConfigInfo);
  }

  public static VaultPKI getVaultPKIInstance(UUID uuid, HashicorpVaultConfigParams configInfo)
      throws Exception {
    LOG.info("Creating vault with : {} ", configInfo.toString());
    VaultAccessor vObj =
        VaultAccessor.buildVaultAccessor(configInfo.vaultAddr, configInfo.vaultToken);
    VaultPKI obj = new VaultPKI(uuid, vObj, configInfo);
    obj.validateRoleParam();
    return obj;
  }

  public static VaultPKI validateVaultConfigParams(HashicorpVaultConfigParams configInfo)
      throws Exception {
    VaultAccessor vObj =
        VaultAccessor.buildVaultAccessor(configInfo.vaultAddr, configInfo.vaultToken);
    VaultPKI obj = new VaultPKI(null, vObj, configInfo);
    obj.validateRoleParam();
    return obj;
  }

  public void validateRoleParam() throws Exception {
    String path = params.mountPath + VaultOperationsForCert.ROLES.toString() + "/" + params.role;

    String allowIPSans = vAccessor.readAt(path, "allow_ip_sans");
    if (Boolean.valueOf(allowIPSans) == false)
      LOG.warn("IP Sans are not allowed with this role {}", params.role);
  }

  public List<Object> getTTL() throws Exception {
    return vAccessor.getTokenExpiryFromVault();
  }

  @Override
  public X509Certificate generateCACertificate(String certLabel, KeyPair keyPair) throws Exception {
    return getCACertificateFromVault();
  }

  @Override
  public CertificateDetails createCertificate(
      String storagePath,
      String username,
      Date certStart,
      Date certExpiry,
      String certFileName,
      String newCertKeyStrName,
      Map<String, Integer> subjectAltNames) {
    LOG.info("Creating certificate for {}, CA:", username, caCertUUID.toString());

    final Map<String, Object> input = new HashMap<>();

    input.put("common_name", username);

    // Handle GeneralName.iPAddress [7]
    String ipNames = CertificateHelper.extractIPsFromGeneralNamesAsString(subjectAltNames);
    if (!Strings.isNullOrEmpty(ipNames)) input.put("ip_sans", ipNames);

    // Handle GeneralName.dNSName [2]
    String nonIPnames = CertificateHelper.extractHostNamesFromGeneralNamesAsString(subjectAltNames);
    if (!Strings.isNullOrEmpty(nonIPnames)) input.put("alt_names", nonIPnames);

    // TODO: how to - other_sans
    // GeneralNames generalNames = CertificateHelper.extractGeneralNames(subjectAltNames);
    // if (generalNames != null)
    //  input.put("other_sans", generalNames.toString());

    if (certStart != null && certExpiry != null) {
      long diffInMillies = Math.abs(certExpiry.getTime() - certStart.getTime());
      long ttlInHrs = TimeUnit.HOURS.convert(diffInMillies, TimeUnit.MILLISECONDS);

      if (ttlInHrs != 0) {
        LOG.info("Setting up ttl as : {}", ttlInHrs);
        String ttlString = String.format("%sh", Long.toString(ttlInHrs));
        input.put("ttl", ttlString);
      }
    }
    try {
      String path = params.mountPath + VaultOperationsForCert.ISSUE.toString() + "/" + params.role;
      Map<String, String> result = vAccessor.writeAt(path, input);

      // fetch certificate
      String newCertPemStr = result.get(ISSUE_FIELD_CERT);
      curCertificateStr = newCertPemStr;
      X509Certificate certObj = CertificateHelper.convertStringToX509Cert(newCertPemStr);
      // fetch key
      String newCertKeyStr = result.get(ISSUE_FIELD_PRV_KEY);
      curKeyStr = newCertKeyStr;
      PrivateKey pKeyObj = CertificateHelper.convertStringToPrivateKey(newCertKeyStr);
      // fetch issue ca cert
      String issuingCAStr = result.get(ISSUE_FIELD_CA);
      curCaCertificateStr = issuingCAStr;
      X509Certificate issueCAcert = CertificateHelper.convertStringToX509Cert(issuingCAStr);

      // String certSerial = result.get("ISSUE_FIELD_SERIAL");
      // String caChain = result.get("ISSUE_FIELD_CA_CHAIN");

      LOG.debug("Issue CA is:: {}", CertificateHelper.getCertificateProperties(issueCAcert));
      LOG.debug("Certificate is:: {}", CertificateHelper.getCertificateProperties(certObj));

      certObj.verify(issueCAcert.getPublicKey(), "BC");

      // for client certificate: later it is read using CertificateHelper.getClientCertFile
      return CertificateHelper.dumpNewCertsToFile(
          storagePath, certFileName, newCertKeyStrName, certObj, pKeyObj);

    } catch (Exception e) {
      LOG.error("Unable to create certificate for {} using CA {}", username, caCertUUID, e);
      throw new RuntimeException("Unable to get certificate from hashicorp Vault");
    }
  }

  @Override
  public Pair<String, String> dumpCACertBundle(
      String storagePath, UUID customerUUID, UUID caCertUUIDParam) throws Exception {

    // Do not make user of CertificateInfo here, it is passed as param
    List<X509Certificate> list = new ArrayList<X509Certificate>();
    LOG.info("Dumping CA certificate for {}", customerUUID.toString());
    try {
      list.add(getCACertificateFromVault());
      list.addAll(getCAChainFromVault());
      LOG.debug("Total certs in bundle::{}", list.size());
    } catch (Exception e) {
      throw new Exception(" Hashicorp: Failed to extract CA Certificate:" + e.getMessage());
    }
    try {
      String certPath = CertificateHelper.getCACertPath(storagePath, customerUUID, caCertUUIDParam);
      LOG.info("Dumping CA certs @{}", certPath);
      CertificateHelper.writeCertBundleToCertPath(list, certPath);
      return new ImmutablePair<>(certPath, "");

    } catch (Exception e) {
      throw new Exception(" Hashicorp: Failed to dump CA Certificate:" + e.getMessage());
    }
  }

  /**
   * Returns <PKI_MOUNT_PATH>/ca/pem, a single certificate at CA
   *
   * @return
   * @throws Exception
   */
  public X509Certificate getCACertificateFromVault() throws Exception {

    // vault read pki/cert/ca or pki/ca/pem
    String path =
        params.mountPath
            + VaultOperationsForCert.CERT.toString()
            + "/"
            + VaultOperationsForCert.CA_CERT.toString();
    String caCert = vAccessor.readAt(path, ISSUE_FIELD_CERT);
    curCaCertificateStr = caCert;

    return CertificateHelper.convertStringToX509Cert(caCert);
  }

  /**
   * Returns <PKI_MOUNT_PATH>/ca_chain, a single certificate at CA
   *
   * @return
   * @throws Exception
   */
  public List<X509Certificate> getCAChainFromVault() throws Exception {
    // vault read pki/ca_chain or pki/cert/ca_chain
    String caCertChain = getCAChainFromVaultInString();

    if (Strings.isNullOrEmpty(caCertChain)) {
      LOG.debug("No certificate chain found for the CA");
    } else {
      curCaCertificateStr = caCertChain;
      return CertificateHelper.convertStringToX509CertList(caCertChain);
    }
    return new ArrayList<X509Certificate>();
  }

  public String getCAChainFromVaultInString() throws Exception {
    String path =
        params.mountPath
            + VaultOperationsForCert.CERT.toString()
            + "/"
            + VaultOperationsForCert.CA_CHAIN.toString();

    // vault read pki/ca_chain or pki/cert/ca_chain
    String caCertChain = vAccessor.readAt(path, ISSUE_FIELD_CERT);
    caCertChain = caCertChain.replaceAll("^\"+|\"+$", "");
    caCertChain = caCertChain.replace("\\n", System.lineSeparator());
    // LOG.debug("CAchain is: {}", caCertChain);

    return caCertChain;
  }

  public String getCACertificate() throws Exception {

    if (curCaCertificateStr.equals("")) getCACertificateFromVault();
    return curCaCertificateStr;
  }

  // @Override
  public String getCertPEM() {
    return curCertificateStr;
  }

  // @Override
  public String getKeyPEM() {
    return curKeyStr;
  }
}
