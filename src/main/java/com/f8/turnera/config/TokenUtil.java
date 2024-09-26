package com.f8.turnera.config;

import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.f8.turnera.security.domain.entities.User;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

@Component
public class TokenUtil {

	private static String SECRET_KEY;

	@Value("${jwt.secret.key}")
	public void setSecretKey(String name) {
		SECRET_KEY = name;
	}
	
	public static String getSecretKey() {
		return SECRET_KEY;
	}

	public static String generateToken(Authentication authentication) {
		String authorities = authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
				.collect(Collectors.joining(","));
		final Key signingKey = new SecretKeySpec(getSecretKey().getBytes(), SignatureAlgorithm.HS256.getJcaName());

		return Jwts.builder().setSubject(((User) authentication.getPrincipal()).getUsername())
				.claim(SecurityConstants.PERMISSIONS_KEY, authorities)
				.signWith(SignatureAlgorithm.HS256, signingKey).setIssuer("f8")
				.setExpiration(new Date(System.currentTimeMillis() + SecurityConstants.EXPIRATION_TIME * 1000))
				.compact();
	}

	public static UsernamePasswordAuthenticationToken getAuthentication(final String token) {
		Claims claims = getAllClaims(token.replace(SecurityConstants.TOKEN_PREFIX, ""));

		List<GrantedAuthority> authorities = AuthorityUtils
				.commaSeparatedStringToAuthorityList(claims.get(SecurityConstants.PERMISSIONS_KEY).toString());

		org.springframework.security.core.userdetails.User principal = new org.springframework.security.core.userdetails.User(
				claims.getSubject(), "", authorities);

		return new UsernamePasswordAuthenticationToken(principal, token, authorities);
	}

	public static String getUsernameWithoutToken() {
		org.springframework.security.core.userdetails.User user = (org.springframework.security.core.userdetails.User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		return user.getUsername();
	}

	public static Object getClaimByToken(String token, String claimKey) {
		return getAllClaims(token.replace(SecurityConstants.TOKEN_PREFIX, "")).get(claimKey);
	}

	public static Claims getAllClaims(String token) {
		final Key signingKey = new SecretKeySpec(getSecretKey().getBytes(), SignatureAlgorithm.HS256.getJcaName());
		return Jwts.parser().setSigningKey(signingKey).parseClaimsJws(token).getBody();
	}

}