package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.InvalidLengthException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Address;
import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.AddressDTO;
import com.ecommerce.project.repositories.AddressRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressServiceImpl implements AddressService {

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private AddressRepository addressRepository;

    @Override
    public AddressDTO createAddress(AddressDTO addressDTO, User user) {
        validateField(addressDTO.getCountry(), 2, "country");
        validateField(addressDTO.getCity(), 4, "city");
        validateField(addressDTO.getStreet(), 5, "street");
        validateField(addressDTO.getPinCode(), 5, "pin code");
        validateField(addressDTO.getBuildingName(), 5, "building name");
        validateField(addressDTO.getState(), 2, "state");

        Address address = modelMapper.map(addressDTO, Address.class);
        address.setUser(user);

        user.getAddresses().add(address);
        Address savedAddress = addressRepository.save(address);

        return modelMapper.map(savedAddress, AddressDTO.class);
    }

    @Override
    public List<AddressDTO> getAddresses() {
        List<Address> addresses = addressRepository.findAll();
        if (addresses.isEmpty()) {
            throw new ResourceNotFoundException("No addresses available.");
        }

        return addresses.stream()
                .map(address -> modelMapper.map(address, AddressDTO.class))
                .toList();
    }

    @Override
    public AddressDTO getAddressById(Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException("Addresses", "addressId", addressId));

        return modelMapper.map(address, AddressDTO.class);

    }

    @Override
    public List<AddressDTO> getUserAddresses(User user) {
        List<Address> addresses = user.getAddresses();
        if (addresses.isEmpty()) {
            throw new ResourceNotFoundException("No addresses available.");
        }
        return  addresses.stream()
                .map(address -> modelMapper.map(address, AddressDTO.class))
                .toList();
    }

    private void validateField(String value, int minLength, String propertyName) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidLengthException("The value of '" + propertyName + "' cannot be empty.");
        }

        if (value.trim().length() < minLength) {
            throw new InvalidLengthException("The value of '" + propertyName + "' must be at least " + minLength + " characters.");
        }
    }
}